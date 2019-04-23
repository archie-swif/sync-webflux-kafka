package com.banana;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class WorkerApp {

    public static void main(String[] args) {
        SpringApplication springApp = new SpringApplication(WorkerApp.class);
        springApp.setAdditionalProfiles("worker");
        springApp.run(args);
    }

}
