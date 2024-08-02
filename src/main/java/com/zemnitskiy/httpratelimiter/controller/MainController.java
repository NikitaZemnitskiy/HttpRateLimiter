package com.zemnitskiy.httpratelimiter.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MainController {

  @GetMapping("/test")
  public String testRateLimiter() {
    return "Request successful";
  }
}
