package com.foody.api.user;

import com.foody.api.shared.persistence.PersistenceConstants;
import java.util.Set;
import org.springframework.data.domain.Sort;

public final class UserConstants {

	public static final String ADMIN_USERS_PATH = "/admin/users";

	public static final String ROLE_PATH = "/{id}/role";

	public static final String NAME_ATTRIBUTE = "name";

	public static final String EMAIL_ATTRIBUTE = "email";

	public static final String ROLE_ATTRIBUTE = "role";

	public static final Set<String> SORTABLE_PROPERTIES = Set.of(NAME_ATTRIBUTE, EMAIL_ATTRIBUTE,
			PersistenceConstants.CREATED_AT_ATTRIBUTE);

	public static final Sort DEFAULT_SORT = Sort.by(NAME_ATTRIBUTE);

	private UserConstants() {
	}

}
