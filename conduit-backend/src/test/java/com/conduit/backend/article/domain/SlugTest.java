package com.conduit.backend.article.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.conduit.backend.shared.kernel.InvalidInputException;
import org.junit.jupiter.api.Test;

class SlugTest {

    @Test
    void convertsTitleToSlug() {
        assertThat(Slug.from("  How to Train Your Dragon!  ").value()).isEqualTo("how-to-train-your-dragon");
    }

    @Test
    void removesDiacritics() {
        assertThat(Slug.from("Café Crème").value()).isEqualTo("cafe-creme");
    }

    @Test
    void rejectsTitleWithoutLettersOrDigits() {
        assertThatThrownBy(() -> Slug.from("!!!")).isInstanceOf(InvalidInputException.class);
    }
}
