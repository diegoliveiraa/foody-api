package com.foody.api.shared.util;

public final class LikeUtils {

	public static final char ESCAPE = '\\';

	private static final String ESCAPE_TEXT = String.valueOf(ESCAPE);

	private static final String ANY_SEQUENCE = "%";

	private static final String ANY_CHARACTER = "_";

	private LikeUtils() {
	}

	public static String containing(String term) {
		String escaped = term.replace(ESCAPE_TEXT, ESCAPE_TEXT + ESCAPE_TEXT)
			.replace(ANY_SEQUENCE, ESCAPE_TEXT + ANY_SEQUENCE)
			.replace(ANY_CHARACTER, ESCAPE_TEXT + ANY_CHARACTER);
		return ANY_SEQUENCE + escaped + ANY_SEQUENCE;
	}

}
