package org.acurat.customer;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
public class CustomerApplication {

    public static void main(String[] args) {
        SpringApplication.run(CustomerApplication.class, args);
    }

}

@Slf4j
@RestController
class BasicController {

    @GetMapping("/")
    public ResponseEntity<String> root() {
        log.info("Creating customer....");
        return ResponseEntity.ok().build();
    }
}
