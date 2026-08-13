import { useMemo, useState } from 'react';
import { RepoSidebar } from './components/RepoSidebar';
import { ChatPanel } from './components/ChatPanel';
import { SourcesPanel } from './components/SourcesPanel';
import { ThreadList } from './components/ThreadList';
import { ShareDialog } from './components/ShareDialog';
import { RepoPickerDialog } from './components/RepoPickerDialog';
import { AuthScreen } from './components/AuthScreen';
import { useRepos } from './hooks/useRepos';
import { useChat } from './hooks/useChat';
import { useThreads } from './hooks/useThreads';
import { useTheme, type Theme } from './hooks/useTheme';
import { useAuth } from './hooks/useAuth';
import type { Repo } from './api/types';
import { Header } from './components/Header';
import { downloadTextFile, slugifyFilename, threadToMarkdown } from './utils/exportMarkdown';

export default function App() {
  // Theme applies to the login screen too, so it lives above the auth gate.
  const { theme, toggle: toggleTheme } = useTheme();
  const { user, loading: authLoading, login, register, logout } = useAuth();

  if (authLoading) {
    return (
      <div className="flex h-full items-center justify-center bg-white dark:bg-neutral-950">
        <p className="text-sm text-neutral-400 dark:text-neutral-500">Loading…</p>
      </div>
    );
  }

  if (!user) {
    return <AuthScreen onLogin={login} onRegister={register} />;
  }

  return <MainApp userEmail={user.email} onLogout={logout} theme={theme} onToggleTheme={toggleTheme} />;
}

interface MainAppProps {
  userEmail: string;
  onLogout: () => void;
  theme: Theme;
  onToggleTheme: () => void;
}

/**
 * Split out from App so its hooks (useRepos, useChat, all the selection and
 * drawer state) only mount once there's an authenticated user — a fresh
 * instance per login, discarded whole on logout rather than carrying
 * another account's stale repos/threads across the transition.
 */
function MainApp({ userEmail, onLogout, theme, onToggleTheme }: MainAppProps) {
  const { repos, loading, error, submitting, submit, retry, sync, remove } = useRepos();
  const [selected, setSelected] = useState<Repo | null>(null);
  const [highlighted, setHighlighted] = useState<number | null>(null);

  const threads = useThreads(selected?.id ?? null);
  const { messages, loading: historyLoading, pending, send, setFeedback } = useChat(threads.activeId, threads.touch);
  const activeThread = threads.threads.find((t) => t.id === threads.activeId) ?? null;

  // Below the md breakpoint the sidebar and sources panel are off-canvas
  // drawers rather than static columns — these track whether each is open.
  const [sidebarOpen, setSidebarOpen] = useState(false);
  const [sourcesOpen, setSourcesOpen] = useState(false);
  const [shareOpen, setShareOpen] = useState(false);
  const [pickerOpen, setPickerOpen] = useState(false);

  // Closing the sources drawer on repo change: sources link to a specific
  // commit, so a panel left open from a previous repo is meaningless here.
  // Reset during render (not in an effect) to avoid the extra cascading
  // render an effect-based reset would cause.
  const [resetForRepoId, setResetForRepoId] = useState<string | null>(null);
  const selectedId = selected?.id ?? null;
  if (resetForRepoId !== selectedId) {
    setResetForRepoId(selectedId);
    setSourcesOpen(false);
  }

  // Citation highlight is scoped to one conversation's sources, not the repo.
  const [resetForThreadId, setResetForThreadId] = useState<string | null>(null);
  if (resetForThreadId !== threads.activeId) {
    setResetForThreadId(threads.activeId);
    setHighlighted(null);
  }

  function handleSelectRepo(repo: Repo) {
    setSelected(repo);
    setSidebarOpen(false); // a pick on mobile should close the drawer it came from
  }

  function handleSelectThread(id: string) {
    threads.setActiveId(id);
    setSidebarOpen(false);
  }

  // Opens the repo picker rather than creating immediately — even the
  // common single-repo case goes through it, pre-checked with the current
  // repo, so there's one flow instead of two (see RepoPickerDialog).
  function handleCreateThread() {
    setPickerOpen(true);
  }

  async function handleConfirmCreateThread(repoIds: string[]) {
    setPickerOpen(false);
    const thread = await threads.create(repoIds);
    if (thread) setSidebarOpen(false);
  }

  // The active thread's repos, resolved to their names for display
  // ("Ask about X, Y") — repos, not threads.threads, is the source of
  // truth for names since a thread only carries repo ids.
  const activeThreadRepoNames = useMemo(() => {
    if (!activeThread) return [];
    return activeThread.repoIds
      .map((id) => repos.find((r) => r.id === id)?.name)
      .filter((name): name is string => Boolean(name));
  }, [activeThread, repos]);

  // The panel always shows the most recent answer's sources.
  const sources = useMemo(() => {
    const lastAnswer = [...messages]
      .reverse()
      .find((m) => m.role === 'assistant' && m.sources?.length);
    return lastAnswer?.sources ?? [];
  }, [messages]);

  function handleDeleteRepo(id: string) {
    remove(id);
    if (selected?.id === id) setSelected(null);
  }

  function handleCitationClick(index: number) {
    setHighlighted(index);
    document
      .getElementById(`source-${index}`)
      ?.scrollIntoView({ behavior: 'smooth', block: 'center' });
  }

  function handleExport() {
    const title = activeThread?.title ?? 'New conversation';
    downloadTextFile(slugifyFilename(title), threadToMarkdown(title, messages), 'text/markdown');
  }

  const repoReady = selected?.status === 'READY';

  return (
    <div className="flex h-full flex-col bg-white dark:bg-neutral-950">
      <Header
        theme={theme}
        onToggleTheme={onToggleTheme}
        onOpenSidebar={() => setSidebarOpen(true)}
        userEmail={userEmail}
        onLogout={onLogout}
        sourcesButton={
          repoReady && threads.activeId
            ? { count: sources.length, onClick: () => setSourcesOpen(true) }
            : undefined
        }
      />
      <div className="flex min-h-0 flex-1">
        {(sidebarOpen || sourcesOpen) && (
          <div
            className="fixed inset-0 z-30 bg-black/40 dark:bg-black/60 md:hidden"
            onClick={() => {
              setSidebarOpen(false);
              setSourcesOpen(false);
            }}
          />
        )}

        <RepoSidebar
          repos={repos}
          loading={loading}
          submitting={submitting}
          error={error ?? threads.error}
          selectedId={selected?.id ?? null}
          mobileOpen={sidebarOpen}
          onCloseMobile={() => setSidebarOpen(false)}
          onSelect={handleSelectRepo}
          onSubmit={submit}
          onRetry={retry}
          onSync={sync}
          onDelete={handleDeleteRepo}
          threadsPanel={
            repoReady && selected
              ? {
                  repoId: selected.id,
                  node: (
                    <ThreadList
                      threads={threads.threads}
                      activeId={threads.activeId}
                      loading={threads.loading}
                      onSelect={handleSelectThread}
                      onCreate={handleCreateThread}
                      onRename={threads.rename}
                      onDelete={threads.remove}
                    />
                  ),
                }
              : undefined
          }
        />

        {selected ? (
          repoReady ? (
            threads.activeId ? (
              <>
                <ChatPanel
                  repoNames={activeThreadRepoNames.length > 0 ? activeThreadRepoNames : [selected.name ?? '']}
                  threadTitle={activeThread?.title ?? null}
                  shared={activeThread?.shareToken != null}
                  messages={messages}
                  pending={pending}
                  historyLoading={historyLoading}
                  onSend={send}
                  onCitationClick={handleCitationClick}
                  onFeedback={setFeedback}
                  onOpenShare={() => setShareOpen(true)}
                  onExport={handleExport}
                />
                <SourcesPanel
                  sources={sources}
                  highlightedIndex={highlighted}
                  mobileOpen={sourcesOpen}
                  onCloseMobile={() => setSourcesOpen(false)}
                />
              </>
            ) : (
              <main className="flex flex-1 items-center justify-center">
                <div className="text-center">
                  <p className="text-sm text-neutral-500 dark:text-neutral-400">
                    No conversation selected
                  </p>
                  <button
                    type="button"
                    onClick={handleCreateThread}
                    className="mt-3 cursor-pointer rounded-full bg-blue-600 px-4 py-2 text-xs font-medium text-white dark:bg-blue-500"
                  >
                    Start a new conversation
                  </button>
                </div>
              </main>
            )
          ) : (
            <main className="flex flex-1 items-center justify-center">
              <div className="text-center">
                <p className="text-sm text-neutral-600 dark:text-neutral-400">
                  Indexing {selected.name}
                </p>
                <p className="mt-1 text-xs text-neutral-400 dark:text-neutral-500">
                  This usually takes a few minutes.
                </p>
              </div>
            </main>
          )
        ) : (
          <main className="flex flex-1 items-center justify-center">
            <p className="text-sm text-neutral-400 dark:text-neutral-500">
              Select a repository to start
            </p>
          </main>
        )}
      </div>

      <ShareDialog
        open={shareOpen}
        onClose={() => setShareOpen(false)}
        shareToken={activeThread?.shareToken ?? null}
        onShare={async () => {
          if (threads.activeId) await threads.share(threads.activeId);
        }}
        onUnshare={async () => {
          if (threads.activeId) await threads.unshare(threads.activeId);
        }}
      />

      {selected && (
        <RepoPickerDialog
          open={pickerOpen}
          repos={repos}
          defaultRepoId={selected.id}
          onClose={() => setPickerOpen(false)}
          onConfirm={handleConfirmCreateThread}
        />
      )}
    </div>
  );
}
