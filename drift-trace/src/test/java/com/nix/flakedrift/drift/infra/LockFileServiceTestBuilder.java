package com.nix.flakedrift.drift.infra;

import com.nix.flakedrift.drift.domain.model.FlakeLockEntry;
import com.nix.flakedrift.drift.infra.impl.LockFileService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/**
 * Builds a parse of {@link LockFileService} with the given flake.lock. The working
 * directory is a required dependency (constructor); the lock body is optional and set
 * with {@link #withLock(String)}; the parse runs in {@link #build()}.
 */
public final class LockFileServiceTestBuilder {
    private final Path directory;
    private String lockBody = "";

    public LockFileServiceTestBuilder(Path directory) {
        this.directory = directory;
    }

    public LockFileServiceTestBuilder withLock(String body) {
        this.lockBody = body;
        return this;
    }

    public Map<String, FlakeLockEntry> build() throws IOException {
        if (!lockBody.isEmpty()) {
            Files.writeString(directory.resolve("flake.lock"), lockBody);
        }
        return new LockFileService().readLockFile(directory.resolve("flake.lock"));
    }
}
