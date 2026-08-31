package com.nix.flakedrift.drift.infra;

import com.nix.flakedrift.drift.infra.impl.NixCommandService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * launches real subprocesses via
 * {@code /bin/sh} (no nix, no store): both stdout and stderr are drained
 * concurrently (no deadlock) and the combined output is returned, so an
 * `update`'s stderr progress lands in the history {@code nixOutput}.
 */
class NixCommandServiceTests {

    @Test
    void givenOutputOnBothStreams_whenRun_thenBothAreCaptured() {
        NixCommandService service = new NixCommandService();
        String result = service.run(List.of("/bin/sh", "-c", "echo out; echo err >&2"));

        assertTrue(result.contains("out"), "stdout captured");
        assertTrue(result.contains("err"), "stderr captured (nix update writes progress to stderr)");
    }

    @Test
    void givenOutputOnlyOnStdout_whenRun_thenReturnedTrimmed() {
        NixCommandService service = new NixCommandService();
        String result = service.run(List.of("/bin/sh", "-c", "echo  sha256-hash "));

        assertTrue(result.startsWith("sha256-hash"), "stdout-only command returns clean value");
    }

    @Test
    void givenLargeStderr_whenRun_thenNoDeadlock() {
        // ~100k lines on stderr (bigger than the 64KB pipe buffer) would previously
        // deadlock a sequential stdout-then-stderr reader; concurrent draining must not.
        NixCommandService service = new NixCommandService();
        String result = service.run(List.of("/bin/sh", "-c", "i=0; while [ $i -lt 50000 ]; do echo x >&2; i=$((i+1)); done; echo done"));

        assertTrue(result.contains("done"), "process finished despite large stderr");
    }
}