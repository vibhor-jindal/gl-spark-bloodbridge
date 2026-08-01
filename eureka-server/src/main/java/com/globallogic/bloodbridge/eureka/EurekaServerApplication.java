package com.globallogic.bloodbridge.eureka;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

/**
 * Service registry for all BloodBridge microservices. Every downstream
 * service (Auth, Donor, Request, Matching, Notification, Inventory,
 * Analytics, Rewards) registers here and is discovered dynamically by
 * the API Gateway and by Feign clients — no hard-coded hostnames/ports.
 */
@SpringBootApplication
@EnableEurekaServer
public class EurekaServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(EurekaServerApplication.class, args);
    }
}
