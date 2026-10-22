package com.conduit.backend.comments.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import com.conduit.backend.comments.domain.Comment;

public interface CommentRepository extends JpaRepository<Comment, Long> {
}
