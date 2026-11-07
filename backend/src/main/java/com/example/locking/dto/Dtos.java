package com.example.locking.dto;

import com.example.locking.entity.Seat;
import jakarta.validation.constraints.*;
import java.util.List;

/**
 * All request/response DTOs as nested records (kept in one file for
 * readability).
 */
public final class Dtos {
	private Dtos() {
	}

	public record CreateSeatRequest(@NotBlank String seatNumber, @NotBlank String flightNumber) {
	}

	/**
	 * expectedVersion is only used by optimistic booking: the version the client
	 * last saw.
	 */
	public record BookRequest(@NotBlank String userName, Long expectedVersion) {
	}

	public record DemoRequest(@NotNull Long seatId, @Min(1) @Max(50) int users) {
	}

	public record SeatResponse(Long id, String seatNumber, String flightNumber, String status, String bookedBy,
			Long version) {
		public static SeatResponse of(Seat s) {
			return new SeatResponse(s.getId(), s.getSeatNumber(), s.getFlightNumber(), s.getStatus().name(),
					s.getBookedBy(), s.getVersion());
		}
	}

	public record UserResult(String user, String result, String explanation, long durationMs) {
	}

	public record DemoResponse(String strategy, int users, long successCount, long totalDurationMs,
			SeatResponse finalSeat, List<UserResult> results) {
	}

	public record ApiError(int status, String error, String message) {
	}
}
