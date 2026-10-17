package com.foody.api.me.controller;

import com.foody.api.me.MeConstants;
import com.foody.api.me.dto.MeResponse;
import com.foody.api.me.service.MeService;
import com.foody.api.shared.security.AuthenticatedUser;
import com.foody.api.shared.security.RequiresAuthentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MeController {

	private final MeService meService;

	public MeController(MeService meService) {
		this.meService = meService;
	}

	@GetMapping(MeConstants.PATH)
	@RequiresAuthentication
	public MeResponse me(@AuthenticationPrincipal AuthenticatedUser user) {
		return meService.describe(user.id());
	}

}
