package com.testtask.errorfreetext;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ErrorfreetextApplication {

	public static void main(String[] args) {
		SpringApplication.run(ErrorfreetextApplication.class, args);
	}

}
