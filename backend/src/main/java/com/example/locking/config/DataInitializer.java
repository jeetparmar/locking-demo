package com.example.locking.config;

import com.example.locking.entity.Seat;
import com.example.locking.repository.SeatRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DataInitializer {
	/** Seeds flight FL100 with 6 rows x 6 seats (A-F) when the table is empty. */
	@Bean
	CommandLineRunner seed(SeatRepository repo) {
		return args -> {
			if (repo.count() > 0)
				return;
			for (int row = 1; row <= 6; row++)
				for (char c = 'A'; c <= 'F'; c++)
					repo.save(new Seat(row + "" + c, "FL100"));
		};
	}
}
