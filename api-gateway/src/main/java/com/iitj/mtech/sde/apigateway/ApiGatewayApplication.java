package com.iitj.mtech.sde.apigateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.RedirectServerAuthenticationSuccessHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
						.filters(f -> f.stripPrefix(2))
						.uri("lb://DATA-SERVICE"))
				.route("analytics-service-route", r -> r.path("/api/analytics/**")
						.filters(f -> f.stripPrefix(2))
						.uri("lb://ANALYTICS-SERVICE"))
				.build();
	}

	@EnableWebFluxSecurity
	static class SecurityConfig {
		@Bean
		SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
			return http
					.csrf(ServerHttpSecurity.CsrfSpec::disable)
					.cors(cors -> cors.configurationSource(corsConfigurationSource()))
					.authorizeExchange(auth -> auth
							.pathMatchers("/auth/user", "/", "/login**", "/logout**", "/oauth2/**").permitAll()
							.pathMatchers("/api/data/**").authenticated()
							.anyExchange().permitAll())
					.oauth2Login(oauth2 -> oauth2.authenticationSuccessHandler(new RedirectServerAuthenticationSuccessHandler("http://localhost:3000")))
					.logout(logout -> logout.logoutSuccessHandler((exchange, authentication) -> {
							// Return 200 on logout for SPA
							exchange.getExchange().getResponse().setStatusCode(HttpStatus.OK);
							return Mono.empty();
						}))
					.build();
		}

		@Bean
		CorsConfigurationSource corsConfigurationSource() {
			CorsConfiguration cfg = new CorsConfiguration();
			cfg.setAllowedOrigins(List.of("http://localhost:3000"));
			cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
			cfg.setAllowedHeaders(List.of("*"));
			cfg.setAllowCredentials(true);
			UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
			source.registerCorsConfiguration("/**", cfg);
			return source;
		}
	}

	@RestController
	static class RootController {
		@GetMapping("/")
		public Mono<Void> rootRedirect(org.springframework.web.server.ServerWebExchange exchange) {
			exchange.getResponse().setStatusCode(HttpStatus.FOUND);
			exchange.getResponse().getHeaders().setLocation(URI.create("http://localhost:3000"));
			return exchange.getResponse().setComplete();
		}
	}

	@RestController
	@RequestMapping("/auth")
	static class AuthController {
		@GetMapping("/user")
		public Map<String, Object> user(@AuthenticationPrincipal OAuth2User principal) {
			Map<String, Object> out = new HashMap<>();
			if (principal == null) {
				out.put("authenticated", false);
				return out;
			}
			out.put("authenticated", true);
			out.put("name", principal.getAttribute("name"));
			out.put("email", principal.getAttribute("email"));
			out.put("picture", principal.getAttribute("picture"));
			return out;
		}
	}
}
