package com.zemnitskiy.httpratelimiter.utils;

public class Utils {


  public static long parseBasePeriod(String s) {
    if (s == null || s.isEmpty()) {
      throw new IllegalArgumentException("Input string cannot be null or empty");
    }

    char unit = s.charAt(s.length() - 1);
    String numberPart = s.substring(0, s.length() - 1);

    long multiplier = switch (unit) {
      case 's' -> 1000; // 1 second = 1000 milliseconds
      case 'm' -> 60000; // 1 minute = 60 * 1000 milliseconds
      case 'h' -> 3600000; // 1 hour = 60 * 60 * 1000 milliseconds
      default -> throw new IllegalArgumentException("Invalid time unit: " + unit);
    };

    try {
      long value = Long.parseLong(numberPart);
      return value * multiplier * 1000000;
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException("Invalid number format: " + numberPart);
    }
  }

}
