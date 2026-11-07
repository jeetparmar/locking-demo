package com.example.locking.exception;

public class SeatAlreadyBookedException extends RuntimeException {
	public SeatAlreadyBookedException(String seat, String by) {
		super("Seat " + seat + " is already booked by " + by);
	}
}
