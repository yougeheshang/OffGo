package org.tinkerhub.offgo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@ComponentScan(basePackages = {"org.tinkerhub.offgo"})
@EntityScan(basePackages = {"org.tinkerhub.offgo"})
public class OffGoApplication {

    public static void main(String[] args) {
        SpringApplication.run(OffGoApplication.class, args);
    }

}
