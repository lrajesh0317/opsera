package com.example;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.*;

@SpringBootApplication
@RestController
public class App {
  @GetMapping("/hello")
  public String hello() { return "Hello from CI pipeline!"; }
  public static void main(String[] args) { SpringApplication.run(App.class, args); }
}