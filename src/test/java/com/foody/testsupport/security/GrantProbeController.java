package com.foody.testsupport.security;

import com.foody.api.shared.security.Action;
import com.foody.api.shared.security.RequiresGrant;
import com.foody.api.shared.security.RequiresScreen;
import com.foody.api.shared.security.Screen;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@TestComponent
@RestController
public class GrantProbeController {

	public static final String GRANT_PATH = "/grant-probe/users/update";

	public static final String SCREEN_PATH = "/grant-probe/users";

	@GetMapping(GRANT_PATH)
	@RequiresGrant(screen = Screen.ADMIN_USERS, action = Action.UPDATE)
	public ResponseEntity<Void> updateUsers() {
		return ResponseEntity.noContent().build();
	}

	@GetMapping(SCREEN_PATH)
	@RequiresScreen(Screen.ADMIN_USERS)
	public ResponseEntity<Void> viewUsers() {
		return ResponseEntity.noContent().build();
	}

}
