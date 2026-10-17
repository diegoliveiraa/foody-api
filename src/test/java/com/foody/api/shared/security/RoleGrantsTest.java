package com.foody.api.shared.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;

class RoleGrantsTest {

	@Test
	void customerReachesOnlyTheStoreScreens() {
		Role role = Role.CUSTOMER;

		Set<Screen> screens = RoleGrants.screensOf(role);

		assertThat(screens).containsExactly(Screen.STORE_MENU, Screen.STORE_MY_ORDERS);
	}

	@Test
	void staffReachesDashboardOrdersAndCatalogButNeitherStoreNorUsers() {
		Role role = Role.STAFF;

		Set<Screen> screens = RoleGrants.screensOf(role);

		assertThat(screens).containsExactly(Screen.DASHBOARD_OVERVIEW, Screen.ORDERS_ORDER_LIST,
				Screen.CATALOG_PRODUCTS, Screen.CATALOG_CATEGORIES);
	}

	@Test
	void adminReachesEverythingStaffReachesPlusUsers() {
		Role role = Role.ADMIN;

		Set<Screen> screens = RoleGrants.screensOf(role);

		assertThat(screens).containsAll(RoleGrants.screensOf(Role.STAFF))
			.contains(Screen.ADMIN_USERS)
			.hasSize(RoleGrants.screensOf(Role.STAFF).size() + 1);
	}

	@Test
	void namesAScreenAuthorityAfterTheScreen() {
		Screen screen = Screen.ORDERS_ORDER_LIST;

		String authority = RoleGrants.authority(screen);

		assertThat(authority).isEqualTo("ORDERS_ORDER_LIST");
	}

	@Test
	void namesAnActionAuthorityAfterTheScreenAndTheAction() {
		Screen screen = Screen.ORDERS_ORDER_LIST;

		String authority = RoleGrants.authority(screen, Action.UPDATE_STATUS);

		assertThat(authority).isEqualTo("ORDERS_ORDER_LIST:UPDATE_STATUS");
	}

	@Test
	void grantsEachReachableScreenAndEveryActionOnIt() {
		Role role = Role.CUSTOMER;

		List<GrantedAuthority> authorities = RoleGrants.authoritiesOf(role);

		assertThat(authorities).extracting(GrantedAuthority::getAuthority)
			.containsExactlyInAnyOrder("STORE_MENU", "STORE_MENU:PLACE_ORDER", "STORE_MY_ORDERS",
					"STORE_MY_ORDERS:CANCEL", "STORE_MY_ORDERS:REQUEST_CANCELLATION");
	}

	@Test
	void grantsNothingOnAScreenOutsideTheRole() {
		Role role = Role.STAFF;

		List<GrantedAuthority> authorities = RoleGrants.authoritiesOf(role);

		assertThat(authorities).extracting(GrantedAuthority::getAuthority)
			.isNotEmpty()
			.doesNotContain("ADMIN_USERS", "ADMIN_USERS:UPDATE", "STORE_MENU");
	}

	@Test
	void keysEachScreenUnderItsModule() {
		Screen screen = Screen.STORE_MY_ORDERS;

		String key = screen.key();

		assertThat(key).isEqualTo("store.my-orders");
		assertThat(screen.module().key()).isEqualTo("store");
	}

}
