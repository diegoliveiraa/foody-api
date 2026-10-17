package com.foody.api.shared.security;

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

public final class RoleGrants {

	private static final Set<Screen> CUSTOMER_SCREENS = EnumSet.of(Screen.STORE_MENU, Screen.STORE_MY_ORDERS);

	private static final Set<Screen> STAFF_SCREENS = EnumSet.of(Screen.DASHBOARD_OVERVIEW, Screen.ORDERS_ORDER_LIST,
			Screen.CATALOG_PRODUCTS, Screen.CATALOG_CATEGORIES);

	private static final Set<Screen> ADMIN_ONLY_SCREENS = EnumSet.of(Screen.ADMIN_USERS);

	private static final Map<Role, Set<Screen>> SCREENS_BY_ROLE = Collections.unmodifiableMap(new EnumMap<>(
			Map.of(Role.CUSTOMER, CUSTOMER_SCREENS, Role.STAFF, STAFF_SCREENS, Role.ADMIN, adminScreens())));

	private RoleGrants() {
	}

	public static Set<Screen> screensOf(Role role) {
		return Collections.unmodifiableSet(SCREENS_BY_ROLE.get(role));
	}

	public static List<GrantedAuthority> authoritiesOf(Role role) {
		return screensOf(role).stream()
			.flatMap(RoleGrants::authoritiesOn)
			.<GrantedAuthority>map(SimpleGrantedAuthority::new)
			.toList();
	}

	public static String authority(Screen screen) {
		return screen.name();
	}

	public static String authority(Screen screen, Action action) {
		return authority(screen) + SecurityConstants.AUTHORITY_SEPARATOR + action.name();
	}

	private static Stream<String> authoritiesOn(Screen screen) {
		Stream<String> actions = screen.actions().stream().map(action -> authority(screen, action));
		return Stream.concat(Stream.of(authority(screen)), actions);
	}

	private static Set<Screen> adminScreens() {
		Set<Screen> screens = EnumSet.copyOf(STAFF_SCREENS);
		screens.addAll(ADMIN_ONLY_SCREENS);
		return screens;
	}

}
