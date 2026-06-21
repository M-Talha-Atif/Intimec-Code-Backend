package com.aidentx.taskmanagement;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class TaskManagementApplication {

  public static void main(String[] args) {
    System.setProperty("log4j.configurationFile", "log4j2.json");
    SpringApplication.run(TaskManagementApplication.class, args);
  }
}
