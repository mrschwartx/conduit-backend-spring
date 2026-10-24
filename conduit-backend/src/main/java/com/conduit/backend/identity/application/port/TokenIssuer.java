package com.conduit.backend.identity.application.port;

public interface TokenIssuer {

    String issue(Long userId);
}
