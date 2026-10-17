package com.foody.api.user.controller;

import com.foody.api.shared.pagination.PageResponse;
import com.foody.api.shared.pagination.SortGuard;
import com.foody.api.shared.security.Action;
import com.foody.api.shared.security.AuthenticatedUser;
import com.foody.api.shared.security.RequiresGrant;
import com.foody.api.shared.security.RequiresScreen;
import com.foody.api.shared.security.Role;
import com.foody.api.shared.security.Screen;
import com.foody.api.user.UserConstants;
import com.foody.api.user.dto.AdminUserFilter;
import com.foody.api.user.dto.AdminUserResponse;
import com.foody.api.user.dto.ChangeRoleRequest;
import com.foody.api.user.dto.UserStatus;
import com.foody.api.user.service.AdminUserService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(UserConstants.ADMIN_USERS_PATH)
public class AdminUserController {

	private final AdminUserService adminUsers;

	public AdminUserController(AdminUserService adminUsers) {
		this.adminUsers = adminUsers;
	}

	@GetMapping
	@RequiresScreen(Screen.ADMIN_USERS)
	public PageResponse<AdminUserResponse> list(@RequestParam(required = false) String search,
			@RequestParam(required = false) Role role, @RequestParam(required = false) UserStatus status,
			Pageable pageable) {
		Pageable allowed = SortGuard.requireAllowed(pageable, UserConstants.SORTABLE_PROPERTIES);
		return PageResponse.of(adminUsers.list(new AdminUserFilter(search, role, status), allowed));
	}

	@PatchMapping(UserConstants.ROLE_PATH)
	@RequiresGrant(screen = Screen.ADMIN_USERS, action = Action.UPDATE)
	public AdminUserResponse changeRole(@AuthenticationPrincipal AuthenticatedUser caller, @PathVariable UUID id,
			@Valid @RequestBody ChangeRoleRequest request) {
		return adminUsers.changeRole(caller.id(), id, request.role());
	}

	@PostMapping(UserConstants.DISABLE_PATH)
	@RequiresGrant(screen = Screen.ADMIN_USERS, action = Action.DISABLE)
	public AdminUserResponse disable(@AuthenticationPrincipal AuthenticatedUser caller, @PathVariable UUID id) {
		return adminUsers.disable(caller.id(), id);
	}

	@PostMapping(UserConstants.ENABLE_PATH)
	@RequiresGrant(screen = Screen.ADMIN_USERS, action = Action.DISABLE)
	public AdminUserResponse enable(@AuthenticationPrincipal AuthenticatedUser caller, @PathVariable UUID id) {
		return adminUsers.enable(caller.id(), id);
	}

}
