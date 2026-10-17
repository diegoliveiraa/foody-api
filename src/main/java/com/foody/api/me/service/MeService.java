package com.foody.api.me.service;

import com.foody.api.me.dto.MeModule;
import com.foody.api.me.dto.MeResponse;
import com.foody.api.me.dto.MeScreen;
import com.foody.api.me.dto.MeUser;
import com.foody.api.shared.security.Module;
import com.foody.api.shared.security.Role;
import com.foody.api.shared.security.RoleGrants;
import com.foody.api.shared.security.Screen;
import com.foody.api.user.entity.User;
import com.foody.api.user.service.UserService;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MeService {

	private final UserService userService;

	public MeService(UserService userService) {
		this.userService = userService;
	}

	@Transactional(readOnly = true)
	public MeResponse describe(UUID userId) {
		User user = userService.getActive(userId);
		MeUser summary = new MeUser(user.getId(), user.getName(), user.getEmail());
		return new MeResponse(summary, navigationOf(user.getRole()));
	}

	private List<MeModule> navigationOf(Role role) {
		Set<Screen> screens = RoleGrants.screensOf(role);
		return Arrays.stream(Module.values())
			.map(module -> moduleOf(module, screens))
			.filter(module -> !module.screens().isEmpty())
			.toList();
	}

	private MeModule moduleOf(Module module, Set<Screen> screens) {
		List<MeScreen> visible = screens.stream()
			.filter(screen -> screen.module() == module)
			.map(MeScreen::of)
			.toList();
		return new MeModule(module.key(), module.label(), visible);
	}

}
