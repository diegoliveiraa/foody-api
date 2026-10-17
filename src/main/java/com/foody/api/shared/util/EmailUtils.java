package com.foody.api.shared.util;

import java.util.Locale;

public final class EmailUtils {

	private EmailUtils() {
	}

	public static String normalize(String email) {
		if (email == null) {
			return null;
		}
		return email.strip().toLowerCase(Locale.ROOT);
	}

}
