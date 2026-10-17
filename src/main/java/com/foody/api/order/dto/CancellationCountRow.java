package com.foody.api.order.dto;

import com.foody.api.order.entity.ActorSide;
import com.foody.api.order.entity.CancellationReason;

public interface CancellationCountRow {

	CancellationReason getReason();

	ActorSide getCancelledBy();

	long getOrderCount();

}
