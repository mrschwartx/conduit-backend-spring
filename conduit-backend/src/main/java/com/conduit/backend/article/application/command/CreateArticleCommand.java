package com.conduit.backend.article.application.command;

import java.util.List;

public record CreateArticleCommand(String title, String description, String body, List<String> tagList) {
}
