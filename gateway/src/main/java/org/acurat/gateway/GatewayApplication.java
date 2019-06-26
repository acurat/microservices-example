package org.acurat.gateway;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
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
                .route("order", r -> r.path("/order").filters(f -> f.stripPrefix(1)).uri("http://localhost:9091"))
                .route("customer", r -> r.path("/customer").filters(f -> f.stripPrefix(1)).uri("http://localhost:9092/"))
                .build();
    }

    @Bean
    @Order(Ordered.LOWEST_PRECEDENCE - 1)
    public GlobalFilter logFilter(@Value("${spring.application.name}") String serviceName) {
        return (exchange, chain) -> {
            log.info("Passing through {} - {}", serviceName, exchange.getRequest().getPath());
            return chain.filter(exchange).then(Mono.fromRunnable(() -> {
                log.info("Exiting through {}", serviceName);
            }));
        };
    }

}
