package com.example.medicaldiscoveryserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

@EnableEurekaServer
@SpringBootApplication
public class MedicalDiscoveryServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(MedicalDiscoveryServerApplication.class, args);
    }

}
