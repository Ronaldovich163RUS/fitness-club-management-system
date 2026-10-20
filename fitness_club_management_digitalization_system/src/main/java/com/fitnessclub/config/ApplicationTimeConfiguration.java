package com.fitnessclub.config;

import java.util.TimeZone;

import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fitnessclub.ClubZone;

@Configuration
public class ApplicationTimeConfiguration {

	@Bean
	public Jackson2ObjectMapperBuilderCustomizer jacksonClubTimeZone() {
		return builder -> builder.timeZone(TimeZone.getTimeZone(ClubZone.ZONE));
	}

	@Bean
	public HibernatePropertiesCustomizer hibernateJdbcTimeZone() {
		return hibernateProperties -> hibernateProperties.put("hibernate.jdbc.time_zone", ClubZone.ZONE_ID);
	}
}
