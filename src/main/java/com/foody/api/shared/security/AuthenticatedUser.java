package com.foody.api.shared.security;

import java.io.Serializable;
import java.util.UUID;

public record AuthenticatedUser(UUID id) implements Serializable {
}
