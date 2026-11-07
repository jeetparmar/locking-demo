package com.example.locking.repository;

import com.example.locking.entity.Seat;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface SeatRepository extends JpaRepository<Seat, Long> {
	/**
	 * Generates: SELECT ... FROM seats WHERE id=? FOR UPDATE (row lock held until
	 * tx ends)
	 */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select s from Seat s where s.id = :id")
	Optional<Seat> findByIdForUpdate(@Param("id") Long id);
}
