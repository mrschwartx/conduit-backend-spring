package com.conduit.backend.article.presentation;

import com.conduit.backend.article.application.ArticleService;
import com.conduit.backend.article.presentation.dto.ArticleResponses.TagsResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/tags")
@Tag(name = "Tags")
public class TagController {

    private final ArticleService articleService;

    public TagController(ArticleService articleService) {
        this.articleService = articleService;
    }

    @GetMapping
    @Operation(operationId = "GetTags", summary = "Get tags")
    public TagsResponse tags() {
        return new TagsResponse(articleService.tags());
    }
}
