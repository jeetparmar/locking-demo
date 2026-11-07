package com.example.locking.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "seats", uniqueConstraints = @UniqueConstraint(columnNames = { "flight_number", "seat_number" }))
@Getter
@Setter
@NoArgsConstructor
public class Seat {
	public enum Status {
		AVAILABLE, BOOKED
	}

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	@Column(name = "seat_number", nullable = false)
	private String seatNumber;
	@Column(name = "flight_number", nullable = false)
	private String flightNumber;
	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private Status status = Status.AVAILABLE;
	private String bookedBy;

	/**
	 * Optimistic locking: Hibernate adds "AND version=?" to every UPDATE and
	 * increments it. Starts at 0.
	 */
	@Version
	private Long version;

	public Seat(String seatNumber, String flightNumber) {
		this.seatNumber = seatNumber;
		this.flightNumber = flightNumber;
	}
}
