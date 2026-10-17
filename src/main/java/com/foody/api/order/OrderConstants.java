package com.foody.api.order;

import com.foody.api.order.entity.ActorSide;
import com.foody.api.order.entity.CancellationReason;
import com.foody.api.order.entity.OrderStatus;
import com.foody.api.order.exception.CancellationNotAllowedException;
import com.foody.api.order.exception.PendingCancellationRequestException;
import com.foody.api.shared.exception.BusinessRuleException;
import com.foody.api.shared.route.RouteConstants;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.springframework.data.domain.Sort;

public final class OrderConstants {

	public static final String ORDERS_PATH = "/orders";

	public static final String STATUS_PATH = RouteConstants.ID_PATH + "/status";

	public static final String APPROVE_CANCELLATION_PATH = RouteConstants.CANCELLATION_REQUEST_PATH + "/approve";

	public static final String DECLINE_CANCELLATION_PATH = RouteConstants.CANCELLATION_REQUEST_PATH + "/decline";

	public static final String PLACED_AT_ATTRIBUTE = "placedAt";

	public static final String TOTAL_ATTRIBUTE = "total";

	public static final String NUMBER_ATTRIBUTE = "number";

	public static final String STATUS_ATTRIBUTE = "status";

	public static final String CUSTOMER_ATTRIBUTE = "customer";

	public static final String ORDER_ATTRIBUTE = "order";

	public static final String ACTOR_ATTRIBUTE = "actor";

	public static final Set<String> SORTABLE_PROPERTIES = Set.of(PLACED_AT_ATTRIBUTE, TOTAL_ATTRIBUTE,
			NUMBER_ATTRIBUTE);

	public static final Sort DEFAULT_SORT = Sort.by(Sort.Direction.DESC, PLACED_AT_ATTRIBUTE);

	public static final int STREET_MIN_LENGTH = 2;

	public static final int STREET_MAX_LENGTH = 120;

	public static final int ADDRESS_NUMBER_MIN_LENGTH = 1;

	public static final int ADDRESS_NUMBER_MAX_LENGTH = 10;

	public static final int COMPLEMENT_MAX_LENGTH = 60;

	public static final int NEIGHBORHOOD_MIN_LENGTH = 2;

	public static final int NEIGHBORHOOD_MAX_LENGTH = 60;

	public static final int CITY_MIN_LENGTH = 2;

	public static final int CITY_MAX_LENGTH = 60;

	public static final Pattern ZIP_CODE_PATTERN = Pattern.compile("^\\d{5}-?\\d{3}$");

	public static final String ZIP_CODE_SEPARATOR = "-";

	public static final int ZIP_CODE_PREFIX_LENGTH = 5;

	public static final Pattern ORDER_NUMBER_SEARCH_PATTERN = Pattern.compile("^#?(\\d{1,18})$");

	public static final int ORDER_NUMBER_SEARCH_GROUP = 1;

	public static final int CANCELLATION_NOTE_MAX_LENGTH = 300;

	public static final String NOTE_ATTRIBUTE = "note";

	public static final Map<OrderStatus, OrderStatus> NEXT_STATUS = Map.of(OrderStatus.RECEIVED, OrderStatus.PREPARING,
			OrderStatus.PREPARING, OrderStatus.OUT_FOR_DELIVERY, OrderStatus.OUT_FOR_DELIVERY, OrderStatus.DELIVERED);

	public static final Set<CancellationReason> DELIVERY_CANCELLATION_REASONS = Collections
		.unmodifiableSet(EnumSet.of(CancellationReason.DELIVERY_FAILED));

	public static final Set<CancellationReason> KITCHEN_CANCELLATION_REASONS = Collections
		.unmodifiableSet(Arrays.stream(CancellationReason.values())
			.filter(reason -> reason.allows(ActorSide.RESTAURANT) && !DELIVERY_CANCELLATION_REASONS.contains(reason))
			.collect(Collectors.toCollection(() -> EnumSet.noneOf(CancellationReason.class))));

	public static final Set<CancellationReason> CUSTOMER_CANCELLATION_REASONS = Collections
		.unmodifiableSet(Arrays.stream(CancellationReason.values())
			.filter(reason -> reason.allows(ActorSide.CUSTOMER))
			.collect(Collectors.toCollection(() -> EnumSet.noneOf(CancellationReason.class))));

	public static final Map<ActorSide, Map<OrderStatus, Set<CancellationReason>>> DIRECT_CANCELLATION_REASONS = Map.of(
			ActorSide.CUSTOMER, Map.of(OrderStatus.RECEIVED, CUSTOMER_CANCELLATION_REASONS), ActorSide.RESTAURANT,
			Map.of(OrderStatus.RECEIVED, KITCHEN_CANCELLATION_REASONS, OrderStatus.PREPARING,
					KITCHEN_CANCELLATION_REASONS, OrderStatus.OUT_FOR_DELIVERY, DELIVERY_CANCELLATION_REASONS));

	public static final Map<OrderStatus, Set<CancellationReason>> REQUESTED_CANCELLATION_REASONS = Map
		.of(OrderStatus.PREPARING, CUSTOMER_CANCELLATION_REASONS);

	public static final Map<ActorSide, Supplier<BusinessRuleException>> PENDING_REQUEST_REFUSALS = Map.of(
			ActorSide.CUSTOMER, CancellationNotAllowedException::new, ActorSide.RESTAURANT,
			PendingCancellationRequestException::new);

	public static final Set<OrderStatus> ACTIVE_STATUSES = Collections
		.unmodifiableSet(Arrays.stream(OrderStatus.values())
			.filter(OrderStatus::isActive)
			.collect(Collectors.toCollection(() -> EnumSet.noneOf(OrderStatus.class))));

	public static final Map<ChronoUnit, String> DATE_TRUNC_FIELDS = Map.of(ChronoUnit.HOURS, "hour", ChronoUnit.DAYS,
			"day");

	private OrderConstants() {
	}

}
