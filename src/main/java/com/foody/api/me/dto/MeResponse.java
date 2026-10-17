package com.foody.api.me.dto;

import java.util.List;

public record MeResponse(MeUser user, List<MeModule> modules) {
}
