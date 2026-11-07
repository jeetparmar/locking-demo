package com.example.locking.service;

import com.example.locking.entity.Seat;
import com.example.locking.exception.*;
import com.example.locking.repository.SeatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SeatService {
	private final SeatRepository repo;
	@Value("${demo.work-millis:30}")
	private long workMillis;

	@Transactional
	public Seat create(String seatNumber, String flight) {
		return repo.save(new Seat(seatNumber, flight));
	}

	@Transactional(readOnly = true)
	public List<Seat> findAll() {
		return repo.findAll(Sort.by("id"));
	}

	@Transactional(readOnly = true)
	public Seat get(Long id) {
		return repo.findById(id).orElseThrow(() -> new SeatNotFoundException(id));
	}

	/**
	 * OPTIMISTIC: no DB lock while we work. Hibernate runs UPDATE seats SET
	 * booked_by=?, status=?, version=? WHERE id=? AND version=? If another tx
	 * committed first, 0 rows match -> ObjectOptimisticLockingFailureException.
	 */
	@Transactional
	public Seat bookOptimistic(Long id, String user, Long expectedVersion) {
		Seat s = repo.findById(id).orElseThrow(() -> new SeatNotFoundException(id));
		if (s.getStatus() == Seat.Status.BOOKED)
			throw new SeatAlreadyBookedException(s.getSeatNumber(), s.getBookedBy());
		if (expectedVersion != null && !expectedVersion.equals(s.getVersion()))
			throw new ObjectOptimisticLockingFailureException(Seat.class, id); // client's view is stale
		work();
		s.setStatus(Seat.Status.BOOKED);
		s.setBookedBy(user);
		return repo.saveAndFlush(s); // flush now so a conflict surfaces inside this method
	}

	/**
	 * PESSIMISTIC: SELECT ... FOR UPDATE blocks other writers on this row until we
	 * commit/rollback. Keep the transaction SHORT: waiting callers hold a DB
	 * connection while they block.
	 */
	@Transactional
	public Seat bookPessimistic(Long id, String user) {
		Seat s = repo.findByIdForUpdate(id).orElseThrow(() -> new SeatNotFoundException(id));
		work();
		if (s.getStatus() == Seat.Status.BOOKED)
			throw new SeatAlreadyBookedException(s.getSeatNumber(), s.getBookedBy());
		s.setStatus(Seat.Status.BOOKED);
		s.setBookedBy(user);
		return repo.saveAndFlush(s);
	}

	@Transactional
	public Seat reset(Long id) {
		Seat s = repo.findById(id).orElseThrow(() -> new SeatNotFoundException(id));
		s.setStatus(Seat.Status.AVAILABLE);
		s.setBookedBy(null);
		return repo.saveAndFlush(s);
	}

	private void work() {
		try {
			Thread.sleep(workMillis);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}
}
