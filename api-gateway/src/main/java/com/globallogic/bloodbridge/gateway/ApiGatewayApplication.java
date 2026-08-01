package com.globallogic.bloodbridge.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * Single entry point for all BloodBridge clients. Routes are resolved
 * dynamically against Eureka as each downstream service comes online
 * (see application.yml) — no service is hard-wired to a fixed host/port.
 *
 * JWT validation (Auth Service integration) is added in the commit that
 * introduces auth-service; until then, every route below is open so the
 * gateway can be exercised end-to-end with whatever services exist so far.
 */
@SpringBootApplication
@EnableDiscoveryClient
public class ApiGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }
}
