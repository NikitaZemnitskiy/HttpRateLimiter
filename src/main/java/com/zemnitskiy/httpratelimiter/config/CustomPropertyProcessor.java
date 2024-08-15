package com.zemnitskiy.httpratelimiter.config;

import static com.zemnitskiy.httpratelimiter.utils.Utils.parseBasePeriod;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;

public class CustomPropertyProcessor implements EnvironmentPostProcessor {

  @Override
  public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
    String basePeriod = environment.getProperty("basePeriod");
    if (basePeriod != null) {
      long basePeriodInNanos = parseBasePeriod(basePeriod);
      environment.getSystemProperties().put("basePeriod", basePeriodInNanos);
    }
  }

}
