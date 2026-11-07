package com.example.locking.service;

import com.example.locking.dto.Dtos.*;
import com.example.locking.exception.SeatAlreadyBookedException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.IntStream;

/**
 * Fires N real concurrent requests at the real database. Nothing here is
 * simulated.
 */
@Service
@RequiredArgsConstructor
public class ConcurrentDemoService {
	private final SeatService seats;

	public DemoResponse run(String strategy, Long seatId, int users) {
		seats.reset(seatId);
		boolean optimistic = "optimistic".equals(strategy);
		CountDownLatch gate = new CountDownLatch(1);
		long t0 = System.nanoTime();
		List<UserResult> results;
		try (ExecutorService ex = Executors.newVirtualThreadPerTaskExecutor()) {
			List<Future<UserResult>> futures = IntStream.rangeClosed(1, users)
					.mapToObj(i -> ex.submit(() -> attempt(i, seatId, optimistic, gate))).toList();
			gate.countDown(); // release everyone at once
			results = futures.stream().map(f -> {
				try {
					return f.get();
				} catch (Exception e) {
					throw new IllegalStateException(e);
				}
			}).toList();
		}
		long total = (System.nanoTime() - t0) / 1_000_000;
		long ok = results.stream().filter(r -> r.result().equals("SUCCESS")).count();
		return new DemoResponse(strategy, users, ok, total, SeatResponse.of(seats.get(seatId)), results);
	}

	private UserResult attempt(int i, Long seatId, boolean optimistic, CountDownLatch gate)
			throws InterruptedException {
		String user = "User " + i;
		Long seenVersion = optimistic ? seats.get(seatId).getVersion() : null; // every user reads first
		gate.await();
		long t = System.nanoTime();
		try {
			if (optimistic)
				seats.bookOptimistic(seatId, user, seenVersion);
			else
				seats.bookPessimistic(seatId, user);
			return res(user, "SUCCESS", "Booking completed", t);
		} catch (OptimisticLockingFailureException e) {
			return res(user, "CONFLICT",
					"Version changed: another tx committed first (0 rows updated). A retry would re-read and see BOOKED.",
					t);
		} catch (SeatAlreadyBookedException e) {
			return res(user, "FAILED",
					optimistic ? "Read the seat after it was booked" : "Waited for the row lock, then saw BOOKED", t);
		} catch (PessimisticLockingFailureException e) {
			return res(user, "TIMEOUT", "Could not get the row lock within lock_timeout", t);
		}
	}

	private static UserResult res(String u, String r, String why, long t0) {
		return new UserResult(u, r, why, (System.nanoTime() - t0) / 1_000_000);
	}
}
