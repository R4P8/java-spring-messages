package com.example.myapp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;


@SpringBootApplication
@EnableScheduling
public class MyappApplication {
        
       private static final Logger logger =
            LoggerFactory.getLogger(MyappApplication.class);


	public static void main(String[] args) {
                logger.info("Starting MyApp...");
		SpringApplication.run(MyappApplication.class, args);
                logger.info("MyApp started successfully!");
	}

}
