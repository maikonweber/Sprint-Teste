package com.example.people.config;

import com.example.people.client.NationalizeProperties;
import com.example.people.security.JwtProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({
        JwtProperties.class,
        AdminProperties.class,
        CorsProperties.class,
        NationalizeProperties.class
})
public class PropertiesConfig {
}
