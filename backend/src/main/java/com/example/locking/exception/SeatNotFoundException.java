package com.example.locking.exception;

public class SeatNotFoundException extends RuntimeException {
	public SeatNotFoundException(Long id) {
		super("Seat " + id + " not found");
	}
}
