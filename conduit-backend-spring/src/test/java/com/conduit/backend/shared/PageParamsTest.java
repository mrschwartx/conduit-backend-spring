package com.conduit.backend.shared;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import com.conduit.backend.shared.errors.ApiException;
import com.conduit.backend.shared.validation.PageParams;

class PageParamsTest {

	@Test
	void defaults() {
		PageParams p = PageParams.parse(null, null);
		assertThat(p.offset()).isZero();
		assertThat(p.limit()).isEqualTo(20);
	}

	@Test
	void limitIsClampedToFifty() {
		assertThat(PageParams.parse("5", "500").limit()).isEqualTo(50);
		assertThat(PageParams.parse(null, "99999999999999999999").limit()).isEqualTo(50);
		assertThat(PageParams.parse("5", "500").offset()).isEqualTo(5);
	}

	@Test
	void invalidValuesAreReportedTogether() {
		assertThatThrownBy(() -> PageParams.parse("-1", "0"))
				.isInstanceOfSatisfying(ApiException.class, ex -> {
					assertThat(ex.status()).isEqualTo(422);
					assertThat(ex.errors()).containsKeys("offset", "limit");
				});
		assertThatThrownBy(() -> PageParams.parse("abc", "1.5")).isInstanceOf(ApiException.class);
		assertThatThrownBy(() -> PageParams.parse("", null)).isInstanceOf(ApiException.class);
	}
}
