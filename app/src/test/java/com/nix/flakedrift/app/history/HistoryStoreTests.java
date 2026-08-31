package com.nix.flakedrift.app.history;

import com.nix.flakedrift.drift.dto.UpdateHistoryRunDto;
import com.nix.flakedrift.drift.dto.UpdateResultDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Atomic coverage of the update-history store: one run file per update run, directory-backed. */
class HistoryStoreTests {

    @Test
    void givenEmptyStore_whenReadAll_thenEmpty(@TempDir Path dir) {
        HistoryStore store = new HistoryStore(dir.resolve("history"));
        assertTrue(store.readAll().isEmpty());
    }

    @Test
    void givenTwoRuns_whenAppend_thenPerRunFilesAndChronologicalReadback(@TempDir Path dir) throws Exception {
        Path historyDir = dir.resolve("history");
        HistoryStore store = new HistoryStore(historyDir);

        store.append(UpdateHistoryRunDto.of(
                "2026-08-25T15:33:00Z", "1.0-SNAPSHOT", "/etc/nixos",
                List.of(
                        new UpdateResultDto("flake-03", 3, true, "sha256-beforeA", "sha256-afterA", "updated nixpkgs: revA → revB"),
                        new UpdateResultDto("flake-02", 2, false, "sha256-beforeB", "sha256-beforeB", "no change"))));
        store.append(UpdateHistoryRunDto.of(
                "2026-08-26T16:00:00Z", "1.0-SNAPSHOT", "/etc/nixos",
                List.of(new UpdateResultDto("root", 0, true, "sha256-c0", "sha256-c1", "updated flake-01"))));

        // one file per run, named after the run timestamp
        List<Path> files;
        try (var s = Files.list(historyDir)) {
            files = s.sorted().toList();
        }
        assertEquals(2, files.size());
        assertTrue(files.get(0).getFileName().toString().startsWith("2026-08-25T15:33:00Z"));
        assertTrue(files.get(1).getFileName().toString().startsWith("2026-08-26T16:00:00Z"));

        // chronological readback, newest last
        var runs = store.readAll();
        assertEquals(2, runs.size());
        assertEquals("2026-08-25T15:33:00Z", runs.get(0).timestamp());
        assertEquals("/etc/nixos", runs.get(0).root());
        assertEquals(2, runs.get(0).flakes().size());
        assertEquals(1, runs.get(0).summary().changed());
        assertEquals(1, runs.get(0).summary().unchanged());
        assertEquals(2, runs.get(0).summary().total());

        // each file is a single run object (no enclosing array), readable JSON
        String raw = Files.readString(files.get(0));
        assertTrue(raw.startsWith("{"), "each run file must be a single run object");
        assertTrue(raw.contains("2026-08-25T15:33:00Z"));
        assertTrue(raw.contains("\"flakes\""));
    }
}