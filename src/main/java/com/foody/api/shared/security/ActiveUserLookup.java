package com.foody.api.shared.security;

import java.util.Optional;
import java.util.UUID;

public interface ActiveUserLookup {

	Optional<Role> findActiveRole(UUID userId);

}
