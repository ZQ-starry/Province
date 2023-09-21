package com.sx.server0;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class Server0Application {

    public static void main(String[] args) {
        SpringApplication.run(Server0Application.class, args);
    }

}
