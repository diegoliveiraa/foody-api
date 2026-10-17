package com.foody.seed.order;

import com.foody.api.catalog.entity.Product;
import com.foody.api.user.entity.User;
import java.util.List;
import org.jspecify.annotations.Nullable;

public record SeedCast(@Nullable User fixedCustomer, List<User> customers, List<User> staff, List<Product> menu) {
}
