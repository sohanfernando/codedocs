package com.sohan.codedocs.repository;

import com.sohan.codedocs.entity.ChatThreadRepo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ChatThreadRepoRepository extends JpaRepository<ChatThreadRepo, UUID> {
    List<ChatThreadRepo> findAllByThreadId(UUID threadId);
}
