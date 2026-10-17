package com.foody.seed.order;

import com.foody.api.order.entity.ActorSide;
import com.foody.api.order.entity.BrazilianState;
import com.foody.api.order.entity.CancellationReason;
import com.foody.api.order.entity.OrderStatus;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public final class OrderSeedConstants {

	public static final int ORDER_BASE_COUNT = 50;

	public static final int ACTIVE_ORDERS_PER_STATUS_BASE = 2;

	public static final int MAX_ACTIVE_ORDERS = 12;

	public static final List<OrderStatus> ACTIVE_STATUSES = Arrays.stream(OrderStatus.values())
		.filter(OrderStatus::isActive)
		.toList();

	public static final int RECENTLY_DELIVERED_BASE = 2;

	public static final int PENDING_REQUESTS_BASE = 2;

	public static final int APPROVED_REQUESTS_BASE = 2;

	public static final int DECLINED_REQUESTS_BASE = 2;

	public static final int FIXED_CUSTOMER_ORDER_COUNT = 5;

	public static final int HISTORY_DAYS = 30;

	public static final Duration RECENT_WINDOW = Duration.ofHours(2);

	public static final int DELIVERED_PERCENT = 85;

	public static final int OTHER_REASON_PERCENT = 15;

	public static final int SECONDS_PER_MINUTE = 60;

	public static final List<Integer> ITEM_COUNTS = List.of(1, 1, 1, 2, 2, 2, 3, 3, 4);

	public static final List<OrderStatus> FORWARD_PATH = List.of(OrderStatus.RECEIVED, OrderStatus.PREPARING,
			OrderStatus.OUT_FOR_DELIVERY, OrderStatus.DELIVERED);

	public static final Map<OrderStatus, MinuteRange> STEP_DELAYS = Map.of(OrderStatus.PREPARING, new MinuteRange(1, 5),
			OrderStatus.OUT_FOR_DELIVERY, new MinuteRange(12, 25), OrderStatus.DELIVERED, new MinuteRange(15, 35));

	public static final List<String> CUSTOMER_OTHER_NOTES = List.of("Vou pedir de novo com outro endereço",
			"Escolhi a forma de pagamento errada", "Um amigo já trouxe o jantar");

	public static final List<CancellationReason> CUSTOMER_REASONS = Arrays.stream(CancellationReason.values())
		.filter(reason -> reason.allows(ActorSide.CUSTOMER) && !reason.requiresNote())
		.toList();

	public static final MinuteRange REQUEST_DELAY = new MinuteRange(2, 6);

	public static final MinuteRange DECISION_DELAY = new MinuteRange(1, 4);

	public static final List<String> DECLINE_NOTES = List.of("O pedido já está quase pronto",
			"Já separamos tudo, vai sair em instantes", "O entregador já está a caminho da loja");

	public static final List<String> RESTAURANT_OTHER_NOTES = List.of("Endereço fora da área de entrega",
			"Pagamento não confirmado", "Cliente não atende o telefone");

	public static final String CITY = "São Paulo";

	public static final BrazilianState STATE = BrazilianState.SP;

	public static final int ZIP_CODE_MIN = 1_000_000;

	public static final int ZIP_CODE_MAX = 5_999_999;

	public static final int ZIP_CODE_SUFFIX_DIVISOR = 1_000;

	public static final String ZIP_CODE_FORMAT = "%05d-%03d";

	public static final int ADDRESS_NUMBER_MIN = 1;

	public static final int ADDRESS_NUMBER_MAX = 3_000;

	public static final int COMPLEMENT_PERCENT = 40;

	public static final List<String> NEIGHBORHOODS = List.of("Bela Vista", "Pinheiros", "Vila Mariana", "Moema",
			"Liberdade", "Consolação", "Perdizes", "Tatuapé", "Santana", "Vila Madalena", "Itaim Bibi", "Brooklin",
			"Mooca", "Butantã", "Lapa", "Higienópolis", "Aclimação", "Saúde");

	public static final List<Integer> QUANTITIES = List.of(1, 1, 1, 1, 1, 1, 2, 2, 2, 3);

	private OrderSeedConstants() {
	}

}
