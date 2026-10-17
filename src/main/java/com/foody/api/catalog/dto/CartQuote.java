package com.foody.api.catalog.dto;

import java.math.BigDecimal;
import java.util.List;

public record CartQuote(List<QuotedItem> items, BigDecimal total) {
}
