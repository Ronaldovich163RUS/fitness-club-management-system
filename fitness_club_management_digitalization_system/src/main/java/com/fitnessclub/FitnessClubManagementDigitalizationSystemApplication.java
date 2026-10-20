package com.fitnessclub;

import java.util.TimeZone;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class FitnessClubManagementDigitalizationSystemApplication {

	public static void main(String[] args) {
		TimeZone.setDefault(TimeZone.getTimeZone(ClubZone.ZONE_ID));
		SpringApplication.run(FitnessClubManagementDigitalizationSystemApplication.class, args);
	}
}

