package com.conduit.backend.article.domain;

import java.util.Collection;

public record ArticleSearchCriteria(String tag, Collection<Long> authorIds, Long favoritedByUserId) {
}
