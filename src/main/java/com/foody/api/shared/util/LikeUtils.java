package com.foody.api.shared.util;

import java.util.Locale;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

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

	public static Optional<String> searchPattern(@Nullable String search) {
		return Optional.ofNullable(search)
			.map(String::strip)
			.filter(term -> !term.isEmpty())
			.map(term -> containing(term.toLowerCase(Locale.ROOT)));
	}

}
