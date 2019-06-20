package org.acurat.service1;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.stream.Collectors;

@SpringBootApplication
public class Service1Application {

    public static void main(String[] args) {
        SpringApplication.run(Service1Application.class, args);
    }

}

@Slf4j
@RestController
class BasicController {

    @GetMapping("/")
    public ResponseEntity<String> multiValue(@RequestHeader MultiValueMap<String, String> headers) {
        headers.forEach((key, value) ->
                log.info(String.format("Header '%s' = %s", key, value.stream().collect(Collectors.joining("|")))));

        return new ResponseEntity<>(String.format("Listed %d headers", headers.size()), HttpStatus.OK);
    }
}
