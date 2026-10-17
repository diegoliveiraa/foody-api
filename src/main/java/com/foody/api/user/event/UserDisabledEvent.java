package com.foody.api.user.event;

import java.util.UUID;

public record UserDisabledEvent(UUID userId) {
}
