package com.foody.api.shared.util;

import com.foody.api.shared.persistence.PersistenceConstants;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import java.util.Optional;
import org.hibernate.query.criteria.HibernateCriteriaBuilder;
import org.jspecify.annotations.Nullable;

public final class TextSearchUtils {

	private TextSearchUtils() {
	}

	public static Optional<String> searchTerm(@Nullable String search) {
		return Optional.ofNullable(search).map(String::strip).filter(term -> !term.isEmpty());
	}

	public static Predicate matches(CriteriaBuilder builder, Expression<String> text, String term) {
		Expression<String> boundTerm = ((HibernateCriteriaBuilder) builder).value(term);
		Expression<String> pattern = builder.function(PersistenceConstants.CONTAINS_PATTERN_FUNCTION, String.class,
				boundTerm);
		return builder.like(folded(builder, text), pattern, PersistenceConstants.LIKE_ESCAPE);
	}

	private static Expression<String> folded(CriteriaBuilder builder, Expression<String> text) {
		return builder.function(PersistenceConstants.UNACCENT_FUNCTION, String.class, builder.lower(text));
	}

}
