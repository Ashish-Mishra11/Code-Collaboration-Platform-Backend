package com.executionservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@SpringBootApplication
public class ExecutionserviceApplication {

	public static void main(String[] args) {
		SpringApplication.run(ExecutionserviceApplication.class, args);
	}

}
