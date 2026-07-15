package com.dinesh.geotaskai.backend.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BackendConfig {
    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
