package com.example.locking;

import com.example.locking.dto.Dtos.DemoResponse;
import com.example.locking.entity.Seat;
import com.example.locking.service.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Real PostgreSQL via Testcontainers. Mockito cannot verify locking: row locks,
 * MVCC and "UPDATE ... WHERE version=?" row counts only exist inside a real
 * database engine.
 */
@SpringBootTest
@Testcontainers
class ConcurrentBookingIT {
	@Container
	@ServiceConnection
	static PostgreSQLContainer<?> pg = new PostgreSQLContainer<>("postgres:16-alpine");

	@Autowired
	SeatService seats;
	@Autowired
	ConcurrentDemoService demo;

	@Test
	void optimistic_exactlyOneWins() {
		Seat s = seats.create("T1", "TEST");
		DemoResponse r = demo.run("optimistic", s.getId(), 20);
		assertEquals(1, r.successCount());
		assertEquals("BOOKED", r.finalSeat().status());
	}

	@Test
	void pessimistic_exactlyOneWins_noConflicts() {
		Seat s = seats.create("T2", "TEST");
		DemoResponse r = demo.run("pessimistic", s.getId(), 20);
		assertEquals(1, r.successCount());
		assertTrue(r.results().stream().noneMatch(x -> x.result().equals("CONFLICT")),
				"waiters see BOOKED, never a version conflict");
	}
}
