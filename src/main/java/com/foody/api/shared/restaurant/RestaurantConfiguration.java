package com.foody.api.shared.restaurant;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(RestaurantProperties.class)
public class RestaurantConfiguration {

}
