package com.iitj.mtech.sde.serviceregistry;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

/**
 * ServiceRegistryApplication
 * This service acts as the discovery server (using Netflix Eureka).
 * All other microservices will register themselves here.
 */
@SpringBootApplication
@EnableEurekaServer // This annotation enables the Eureka server dashboard and functionality
public class ServiceRegistryApplication {

	public static void main(String[] args) {
		SpringApplication.run(ServiceRegistryApplication.class, args);
	}

}
