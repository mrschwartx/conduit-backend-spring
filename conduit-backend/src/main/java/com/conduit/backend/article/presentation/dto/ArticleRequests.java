package com.conduit.backend.article.presentation.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

public final class ArticleRequests {

    private ArticleRequests() {
    }

    public record NewArticleRequest(@NotNull(message = "can't be blank") @Valid NewArticleBody article) {
    }

    public record NewArticleBody(
            @NotBlank(message = "can't be blank") @Size(max = 255, message = "is too long") String title,
            @NotBlank(message = "can't be blank") @Size(max = 1000, message = "is too long") String description,
            @NotBlank(message = "can't be blank") String body,
            List<@NotBlank(message = "can't be blank") @Size(max = 100, message = "is too long") String> tagList) {
    }

    public record UpdateArticleRequest(@NotNull(message = "can't be blank") @Valid UpdateArticleBody article) {
    }

    public record UpdateArticleBody(
            @Pattern(regexp = ".*\\S.*", message = "can't be blank") @Size(max = 255, message = "is too long") String title,
            @Pattern(regexp = ".*\\S.*", message = "can't be blank") @Size(max = 1000, message = "is too long") String description,
            @Pattern(regexp = "(?s).*\\S.*", message = "can't be blank") String body,
            List<@NotBlank(message = "can't be blank") @Size(max = 100, message = "is too long") String> tagList) {
    }
}
