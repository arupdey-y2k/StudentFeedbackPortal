package com.iitj.mtech.sde.apigateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;

/**
 * ApiGatewayApplication
 * This service acts as the single entry point for all client requests.
 * It uses Spring Cloud Gateway to route traffic to the appropriate microservices.
 */
@SpringBootApplication
@EnableDiscoveryClient // Enables service discovery capabilities
public class ApiGatewayApplication {

	public static void main(String[] args) {
		SpringApplication.run(ApiGatewayApplication.class, args);
	}

	/**
	 * Configures the routes for the gateway.
	 * All requests to /api/data/** will be routed to the data-service.
	 * All requests to /api/analytics/** will be routed to the analytics-service.
	 */
	@Bean
	public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
		return builder.routes()
				.route("data-service-route", r -> r.path("/api/data/**")
						.filters(f -> f.stripPrefix(2)) // Strips /api/data/
						.uri("lb://DATA-SERVICE")) // "lb" means load-balanced, DATA-SERVICE is the name from Eureka

				.route("analytics-service-route", r -> r.path("/api/analytics/**")
						.filters(f -> f.stripPrefix(2)) // Strips /api/analytics/
						.uri("lb://ANALYTICS-SERVICE"))
				.build();
	}
}
