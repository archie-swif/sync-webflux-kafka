package com.banana;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ManagerApp {

    public static void main(String[] args) {
        SpringApplication springApp = new SpringApplication(ManagerApp.class);
        springApp.setAdditionalProfiles("manager");
        springApp.run(args);
    }

}
