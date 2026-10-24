package com.conduit.backend.article.domain;

import com.conduit.backend.shared.kernel.InvalidInputException;
import java.text.Normalizer;
import java.util.Locale;

public record Slug(String value) {

    public static Slug from(String title) {
        String normalized = Normalizer.normalize(title, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        String slug = normalized.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-+)|(-+$)", "");
        if (slug.isEmpty()) {
            throw new InvalidInputException("title", "must contain letters or digits");
        }
        return new Slug(slug);
    }
}
