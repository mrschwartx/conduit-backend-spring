package com.conduit.backend.articles.web;

import java.util.List;

/** Response envelope {"tags": [...]}. */
public record TagsEnvelope(List<String> tags) {
}
