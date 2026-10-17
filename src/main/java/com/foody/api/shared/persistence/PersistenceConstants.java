package com.foody.api.shared.persistence;

import java.time.temporal.ChronoUnit;

public final class PersistenceConstants {

	public static final String DELETED_AT_COLUMN = "deleted_at";

	public static final String ID_ATTRIBUTE = "id";

	public static final String CREATED_AT_ATTRIBUTE = "createdAt";

	public static final String DISABLED_AT_ATTRIBUTE = "disabledAt";

	public static final ChronoUnit TIMESTAMP_PRECISION = ChronoUnit.MICROS;

	private PersistenceConstants() {
	}

}
