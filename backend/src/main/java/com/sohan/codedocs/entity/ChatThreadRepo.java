package com.sohan.codedocs.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * One (thread, repo) pairing — a thread with N repos has N rows here. A
 * synthetic id rather than a composite (thread_id, repo_id) key: JPA's
 * composite-key machinery (@IdClass/@EmbeddedId) buys nothing for a table
 * this simple and this app never looks one up by anything but thread_id
 * or repo_id individually.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "chat_thread_repos", indexes = {
        @Index(name = "idx_thread_repos_thread", columnList = "thread_id"),
        @Index(name = "idx_thread_repos_repo", columnList = "repo_id")
})
public class ChatThreadRepo extends BaseEntity {

    @Column(name = "thread_id", nullable = false)
    private UUID threadId;

    @Column(name = "repo_id", nullable = false)
    private UUID repoId;
}
