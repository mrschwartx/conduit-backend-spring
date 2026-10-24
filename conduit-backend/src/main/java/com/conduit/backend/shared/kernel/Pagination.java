package com.conduit.backend.shared.kernel;

public record Pagination(int offset, int limit) {

    public static final int DEFAULT_LIMIT = 20;

    public static Pagination of(Integer offset, Integer limit) {
        int resolvedOffset = offset == null ? 0 : offset;
        int resolvedLimit = limit == null ? DEFAULT_LIMIT : limit;
        if (resolvedOffset < 0) {
            throw new InvalidInputException("offset", "must be greater than or equal to 0");
        }
        if (resolvedLimit < 1) {
            throw new InvalidInputException("limit", "must be greater than or equal to 1");
        }
        return new Pagination(resolvedOffset, resolvedLimit);
    }
}
