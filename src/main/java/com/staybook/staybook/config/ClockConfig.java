package com.staybook.staybook.config;

import java.time.Clock;
import java.time.ZoneId;
import java.util.TimeZone;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ClockConfig {

    @Bean
    public Clock clock() {
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
        return Clock.system(ZoneId.of("Asia/Ho_Chi_Minh"));
    }
}
