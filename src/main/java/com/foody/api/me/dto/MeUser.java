package com.foody.api.me.dto;

import java.util.UUID;

public record MeUser(UUID id, String name, String email) {
}
