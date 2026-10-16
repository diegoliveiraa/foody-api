package com.foody.seed;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "foody.seed")
public record SeedProperties(@DefaultValue("SMALL") SeedScale scale, @DefaultValue("42") long randomSeed) {
}
