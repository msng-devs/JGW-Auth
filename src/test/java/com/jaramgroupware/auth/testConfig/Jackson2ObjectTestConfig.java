package com.jaramgroupware.auth.testConfig;

import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@TestConfiguration
public class Jackson2ObjectTestConfig {
    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jackson2ObjectMapperBuilderCustomizer(){
        return jacksonObjectMapperBuilder -> {
            jacksonObjectMapperBuilder.simpleDateFormat("yyyy-MM-dd HH:mm:ss");
        };
    }
}
