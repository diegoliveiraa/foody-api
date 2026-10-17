package com.foody.api.user;

import java.util.Set;
import org.springframework.data.domain.Sort;

public final class UserConstants {

	public static final String ADMIN_USERS_PATH = "/admin/users";

	public static final String ROLE_PATH = "/{id}/role";

	public static final String DISABLE_PATH = "/{id}/disable";

	public static final String ENABLE_PATH = "/{id}/enable";

	public static final String ID_ATTRIBUTE = "id";

	public static final String NAME_ATTRIBUTE = "name";

	public static final String EMAIL_ATTRIBUTE = "email";

	public static final String ROLE_ATTRIBUTE = "role";

	public static final String CREATED_AT_ATTRIBUTE = "createdAt";

	public static final String DISABLED_AT_ATTRIBUTE = "disabledAt";

	public static final Set<String> SORTABLE_PROPERTIES = Set.of(NAME_ATTRIBUTE, EMAIL_ATTRIBUTE, CREATED_AT_ATTRIBUTE);

	public static final Sort DEFAULT_SORT = Sort.by(NAME_ATTRIBUTE);

	public static final Sort TIEBREAKER_SORT = Sort.by(ID_ATTRIBUTE);

	private UserConstants() {
	}

}
