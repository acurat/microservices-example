package org.acurat.gateway;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.distribution.pause.ClockDriftPauseDetector;
import io.micrometer.core.instrument.distribution.pause.NoPauseDetector;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.AbstractServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Duration;

import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR;

@Slf4j
@SpringBootApplication
public class GatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }

    @Bean
    public RouteLocator routeLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("order", r -> r.path("/order")
                        .filters(f -> f.stripPrefix(1))
                        .uri("http://localhost:9091"))
                .route("customer", r -> r.path("/customer")
                        .filters(f -> f.stripPrefix(1))
                        .uri("http://localhost:9092/"))
                .build();
    }

    @Bean
    public GlobalFilter logFilter(@Value("${spring.application.name}") String serviceName) {
        return (exchange, chain) -> {
            log.info("Passing through {} - {}", serviceName, exchange.getRequest().getPath());
            return chain.filter(exchange).doOnSuccessOrError((s, e) -> {
                log.info("Exiting through {}", serviceName);
            });
        };
    }

}

@Component
class CustomGatewayMetricsFilter implements GlobalFilter, Ordered {

    private final Log log = LogFactory.getLog(getClass());

    private final MeterRegistry meterRegistry;

    public CustomGatewayMetricsFilter(MeterRegistry meterRegistry) {
        meterRegistry.config().pauseDetector(new ClockDriftPauseDetector(Duration.ofMillis(100), Duration.ofMillis(100)));
        this.meterRegistry = meterRegistry;
    }

    @Override
    public int getOrder() {
        // start the timer as soon as possible and report the metric event before we write
        // response to client
        return Ordered.HIGHEST_PRECEDENCE + 10000;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        Timer.Sample sample = Timer.start(meterRegistry);

        return chain.filter(exchange).doOnSuccessOrError((aVoid, ex) -> {
            endTimerRespectingCommit(exchange, sample);
        });
    }

    private void endTimerRespectingCommit(ServerWebExchange exchange, Timer.Sample sample) {

        ServerHttpResponse response = exchange.getResponse();
        if (response.isCommitted()) {
            endTimerInner(exchange, sample);
        }
        else {
            response.beforeCommit(() -> {
                endTimerInner(exchange, sample);
                return Mono.empty();
            });
        }
    }

    private void endTimerInner(ServerWebExchange exchange, Timer.Sample sample) {
        String outcome = "CUSTOM";
        String status = "CUSTOM";
        HttpStatus statusCode = exchange.getResponse().getStatusCode();
        if (statusCode != null) {
            outcome = statusCode.series().name();
            status = statusCode.name();
        }
        else { // a non standard HTTPS status could be used. Let's be defensive here
            if (exchange.getResponse() instanceof AbstractServerHttpResponse) {
                Integer statusInt = ((AbstractServerHttpResponse) exchange.getResponse())
                        .getStatusCodeValue();
                if (statusInt != null) {
                    status = String.valueOf(statusInt);
                }
                else {
                    status = "NA";
                }
            }
        }
        Route route = exchange.getAttribute(GATEWAY_ROUTE_ATTR);
        Tags tags = Tags.of("outcome", outcome, "status", status, "routeId",
                route.getId(), "routeUri", route.getUri().toString());
        if (log.isTraceEnabled()) {
            log.trace("Stopping timer 'gateway.requests1' with tags " + tags);
        }
        sample.stop(meterRegistry.timer("gateway.requests1", tags));
    }

}
