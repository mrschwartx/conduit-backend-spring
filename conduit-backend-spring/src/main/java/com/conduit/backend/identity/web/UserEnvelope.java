package com.conduit.backend.identity.web;

import com.conduit.backend.identity.application.UserView;

/** Response envelope {"user": {...}}. */
public record UserEnvelope(UserView user) {
}
