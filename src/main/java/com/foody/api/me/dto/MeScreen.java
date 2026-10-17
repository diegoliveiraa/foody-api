package com.foody.api.me.dto;

import com.foody.api.shared.security.Action;
import com.foody.api.shared.security.Screen;
import java.util.List;

public record MeScreen(String key, String label, List<Action> actions) {

	public static MeScreen of(Screen screen) {
		return new MeScreen(screen.key(), screen.label(), screen.actions());
	}

}
