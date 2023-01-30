package com.jaramgroupware.auth.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;
import java.util.TimeZone;

@Configuration
public class Jackson2ObjectConfig{

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jackson2ObjectMapperBuilderCustomizer(){
        return jacksonObjectMapperBuilder -> {
            jacksonObjectMapperBuilder.simpleDateFormat("yyyy-MM-dd HH:mm:ss");
        };
    }
}
