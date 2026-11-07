package com.example.locking.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.*;

/**
 * Lets index.html (opened from file:// or another port) call the API. Demo
 * only.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {
	@Override
	public void addCorsMappings(CorsRegistry r) {
		r.addMapping("/api/**").allowedOrigins("*").allowedMethods("*");
	}
}
