package com.foody.api.shared.money;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class MoneyConstants {

	public static final int SCALE = 2;

	public static final RoundingMode ROUNDING = RoundingMode.HALF_EVEN;

	public static final BigDecimal ZERO = BigDecimal.ZERO.setScale(SCALE);

	private MoneyConstants() {
	}

}
