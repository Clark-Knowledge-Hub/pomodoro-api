package com.pomodoro;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class PomodoroApplicationTests {

	@Test
	void mainClassExists() {
		assertDoesNotThrow(() -> Class.forName("com.pomodoro.PomodoroApplication"));
	}

}
