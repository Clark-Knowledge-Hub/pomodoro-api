package com.pomodoro.controller;

import com.pomodoro.dto.SessionCreateDTO;
import com.pomodoro.dto.SessionDetailDTO;
import com.pomodoro.dto.SessionFilter;
import com.pomodoro.dto.SessionSummaryDTO;
import com.pomodoro.service.SessionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/sessions")
@RequiredArgsConstructor
public class SessionController {

    private final SessionService sessionService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SessionDetailDTO create(@Valid @RequestBody SessionCreateDTO dto) {
        return sessionService.save(dto);
    }

    @GetMapping
    public Page<SessionSummaryDTO> findAll(
            SessionFilter filter,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "date") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir
    ) {
        Sort sort = sortDir.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(page, size, sort);
        return sessionService.findAll(filter, pageable);
    }

    @GetMapping("/{id}")
    public SessionDetailDTO findById(@PathVariable String id) {
        return sessionService.findById(id);
    }
}