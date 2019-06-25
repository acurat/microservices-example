package org.acurat.gateway;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import reactor.core.publisher.Mono;

@Slf4j
@SpringBootApplication
public class GatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }

    @Bean
    public RouteLocator routeLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("proxy1", r -> r.path("/**").uri("http://localhost:9092"))
                .build();
    }

    @Bean
    public GlobalFilter logFilter(@Value("${spring.application.name}") String serviceName) {
        return (exchange, chain) -> {
            log.info("Passing through {}", serviceName);
            return chain.filter(exchange).then(Mono.fromRunnable(() -> {
                log.info("Exiting through {}", serviceName);
            }));
        };
    }

}
