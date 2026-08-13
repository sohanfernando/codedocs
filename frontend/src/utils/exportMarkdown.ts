import type { Message } from '../hooks/useChat';

/** Builds a self-contained Markdown document from a conversation. */
export function threadToMarkdown(title: string, messages: Message[]): string {
  const lines: string[] = [`# ${title}`, ''];

  for (const message of messages) {
    if (message.role === 'user') {
      lines.push(`### Q: ${message.content}`, '');
      continue;
    }

    lines.push(message.content, '');

    if (message.sources && message.sources.length > 0) {
      lines.push('**Sources:**', '');
      for (const source of message.sources) {
        lines.push(
          `- [\`${source.filePath}:${source.startLine}-${source.endLine}\`](${source.githubUrl}) ` +
            `(${Math.round(source.similarity * 100)}% match)`,
        );
      }
      lines.push('');
    }
  }

  return lines.join('\n');
}

/** Triggers a browser download — this is the app's own page, not a sandboxed artifact. */
export function downloadTextFile(filename: string, content: string, mimeType: string) {
  const blob = new Blob([content], { type: `${mimeType};charset=utf-8` });
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = filename;
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
  URL.revokeObjectURL(url);
}

/** Filesystem-safe filename derived from a conversation title. */
export function slugifyFilename(title: string): string {
  const slug = title
    .toLowerCase()
    .trim()
    .replace(/[^a-z0-9]+/g, '-')
    .replace(/^-+|-+$/g, '');
  return (slug || 'conversation') + '.md';
}
