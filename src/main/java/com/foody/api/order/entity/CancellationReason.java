package com.foody.api.order.entity;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

public enum CancellationReason {

	CHANGED_MIND(EnumSet.of(ActorSide.CUSTOMER), false), ORDERED_BY_MISTAKE(EnumSet.of(ActorSide.CUSTOMER), false),
	TAKING_TOO_LONG(EnumSet.of(ActorSide.CUSTOMER), false), ITEM_UNAVAILABLE(EnumSet.of(ActorSide.RESTAURANT), false),
	STORE_CLOSED(EnumSet.of(ActorSide.RESTAURANT), false), CUSTOMER_REQUEST(EnumSet.of(ActorSide.RESTAURANT), false),
	DELIVERY_FAILED(EnumSet.of(ActorSide.RESTAURANT), false), OTHER(EnumSet.allOf(ActorSide.class), true);

	private final Set<ActorSide> sides;

	private final boolean requiresNote;

	CancellationReason(Set<ActorSide> sides, boolean requiresNote) {
		this.sides = Collections.unmodifiableSet(sides);
		this.requiresNote = requiresNote;
	}

	public boolean allows(ActorSide side) {
		return sides.contains(side);
	}

	public boolean requiresNote() {
		return requiresNote;
	}

}
