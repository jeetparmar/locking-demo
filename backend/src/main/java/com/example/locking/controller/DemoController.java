package com.example.locking.controller;

import com.example.locking.dto.Dtos.*;
import com.example.locking.service.ConcurrentDemoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/demo/concurrent-booking")
@RequiredArgsConstructor
public class DemoController {
	private final ConcurrentDemoService demo;

	@PostMapping("/optimistic")
	public DemoResponse optimistic(@Valid @RequestBody DemoRequest r) {
		return demo.run("optimistic", r.seatId(), r.users());
	}

	@PostMapping("/pessimistic")
	public DemoResponse pessimistic(@Valid @RequestBody DemoRequest r) {
		return demo.run("pessimistic", r.seatId(), r.users());
	}
}
