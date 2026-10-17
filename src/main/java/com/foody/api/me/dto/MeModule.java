package com.foody.api.me.dto;

import java.util.List;

public record MeModule(String key, String label, List<MeScreen> screens) {
}
