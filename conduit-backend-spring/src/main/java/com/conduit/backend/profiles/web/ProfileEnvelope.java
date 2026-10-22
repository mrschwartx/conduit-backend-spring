package com.conduit.backend.profiles.web;

import com.conduit.backend.profiles.application.ProfileView;

/** Response envelope {"profile": {...}}. */
public record ProfileEnvelope(ProfileView profile) {
}
