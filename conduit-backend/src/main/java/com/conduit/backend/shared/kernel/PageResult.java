package com.conduit.backend.shared.kernel;

import java.util.List;

public record PageResult<T>(List<T> items, long total) {

    public static <T> PageResult<T> empty() {
        return new PageResult<>(List.of(), 0);
    }
}
