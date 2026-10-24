package com.conduit.backend.article.contract;

import java.util.Optional;

public interface ArticleLookup {

    Optional<Long> findIdBySlug(String slug);
}
