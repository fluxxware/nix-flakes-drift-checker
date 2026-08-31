package com.nix.flakedrift.drift.infra;

import com.nix.flakedrift.drift.domain.model.FlakeLockEntry;
import com.nix.flakedrift.drift.infra.impl.LockFileService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static com.nix.flakedrift.drift.infra.LockFileServiceTestData.GITHUB;
import static com.nix.flakedrift.drift.infra.LockFileServiceTestData.MALFORMED;
import static com.nix.flakedrift.drift.infra.LockFileServiceTestData.PATH_ONLY;
import static com.nix.flakedrift.drift.infra.LockFileServiceTestData.WITH_NARHASH;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Parsing tests for {@link LockFileService}. */
class LockFileServiceTests {

    @Test
    void givenPathEntryWithNarHash_whenParsingLock_thenHashPathTypeExposed(@TempDir Path dir) throws IOException {
        String expectedLeafAppName = "leaf-app";
        String expectedNarHash = "sha256-Kt3+iZEzLAQaMVzW+hcNmZX9VlaBqxYaDLzb7V6+SkU=";
        String expectedPath = "/fixtures/leaf-app";
        String expectedType = "path";

        Map<String, FlakeLockEntry> parsedEntries = new LockFileServiceTestBuilder(dir)
                        .withLock(WITH_NARHASH)
                        .build();

        FlakeLockEntry leafAppEntry = parsedEntries.get(expectedLeafAppName);
        assertEquals(expectedNarHash, leafAppEntry.narHash());
        assertEquals(expectedPath, leafAppEntry.path());
        assertEquals(expectedType, leafAppEntry.type());
    }

    @Test
    void givenPathOnlyEntry_whenParsingLock_thenNoNarHash(@TempDir Path dir) throws IOException {
        String expectedAggregatorName = "aggregator";
        String expectedPath = "./aggregator";

        Map<String, FlakeLockEntry> parsedEntries = new LockFileServiceTestBuilder(dir)
                        .withLock(PATH_ONLY)
                        .build();

        FlakeLockEntry aggregatorEntry = parsedEntries.get(expectedAggregatorName);
        assertNull(aggregatorEntry.narHash());
        assertEquals(expectedPath, aggregatorEntry.path());
    }

    @Test
    void givenGithubEntry_whenParsingLock_thenNoPathAndTypeGithub(@TempDir Path dir) throws IOException {
        String expectedUpstreamName = "nixpkgs";
        String expectedType = "github";

        Map<String, FlakeLockEntry> parsedEntries = new LockFileServiceTestBuilder(dir)
                        .withLock(GITHUB)
                        .build();

        FlakeLockEntry upstreamEntry = parsedEntries.get(expectedUpstreamName);
        assertEquals(expectedType, upstreamEntry.type());
        assertNull(upstreamEntry.path());
    }

    @Test
    void givenNoLockFile_whenParsingLock_thenEmpty(@TempDir Path dir) throws IOException {
        Map<String, FlakeLockEntry> parsedEntries = new LockFileServiceTestBuilder(dir)
                .build();
        assertTrue(parsedEntries.isEmpty());
    }

    @Test
    void givenMalformedJson_whenParsingLock_thenThrows(@TempDir Path dir) throws IOException {
        Path lockPath = dir.resolve("flake.lock");
        Files.writeString(lockPath, MALFORMED);
        assertThrows(IllegalStateException.class, () -> new LockFileService().readLockFile(lockPath));
    }
}
