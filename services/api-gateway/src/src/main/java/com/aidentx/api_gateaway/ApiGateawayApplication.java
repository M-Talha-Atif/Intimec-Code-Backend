package com.aidentx.api_gateaway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ApiGateawayApplication {

	public static void main(String[] args) {

		System.setProperty("log4j.configurationFile", "log4j2.json");
		SpringApplication.run(ApiGateawayApplication.class, args);
	}

}
