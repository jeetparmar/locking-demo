package com.example.locking.controller;

import com.example.locking.dto.Dtos.*;
import com.example.locking.service.SeatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/seats")
@RequiredArgsConstructor
public class SeatController {
	private final SeatService service;

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public SeatResponse create(@Valid @RequestBody CreateSeatRequest r) {
		return SeatResponse.of(service.create(r.seatNumber(), r.flightNumber()));
	}

	@GetMapping
	public List<SeatResponse> all() {
		return service.findAll().stream().map(SeatResponse::of).toList();
	}

	@GetMapping("/{id}")
	public SeatResponse one(@PathVariable Long id) {
		return SeatResponse.of(service.get(id));
	}

	@PostMapping("/{id}/book/optimistic")
	public SeatResponse bookOptimistic(@PathVariable Long id, @Valid @RequestBody BookRequest r) {
		return SeatResponse.of(service.bookOptimistic(id, r.userName(), r.expectedVersion()));
	}

	@PostMapping("/{id}/book/pessimistic")
	public SeatResponse bookPessimistic(@PathVariable Long id, @Valid @RequestBody BookRequest r) {
		return SeatResponse.of(service.bookPessimistic(id, r.userName()));
	}

	@PostMapping("/{id}/reset")
	public SeatResponse reset(@PathVariable Long id) {
		return SeatResponse.of(service.reset(id));
	}
}
