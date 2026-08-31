package com.nix.flakedrift.app.history;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.nix.flakedrift.drift.dto.UpdateHistoryRunDto;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * JSON-backed update-history store: one pretty-printed {@link UpdateHistoryRunDto}
 * per {@code update} RUN, stored as {@code <dir>/<timestamp>.json}. Appends are
 * atomic (a new file per run — old runs are never rewritten), so reads just walk
 * the directory, newest-last.
 *
 * <pre>{@code
 * ~/.flake-drift/
 * ├── 2026-08-25T15:33:00.590246152Z.json   { "timestamp": "...", "flakes": [...], "summary": {...} }
 * └── 2026-08-26T09:12:00.123456789Z.json
 * }</pre>
 */
public final class HistoryStore {
    private final Path directory;
    private final ObjectMapper mapper = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);

    public HistoryStore(Path directory) {
        this.directory = directory;
    }

    public void append(UpdateHistoryRunDto run) {
        try {
            Files.createDirectories(directory);
            mapper.writeValue(runFile(run).toFile(), run);
        } catch (IOException e) {
            throw new IllegalStateException("cannot write history: " + runFile(run), e);
        }
    }

    public List<UpdateHistoryRunDto> readAll() {
        if (!Files.isDirectory(directory)) {
            return List.of();
        }
        try (Stream<Path> files = Files.list(directory)) {
            return files
                    .filter(path -> path.getFileName().toString().endsWith(".json"))
                    .sorted(Comparator.comparing(Path::getFileName)) // ISO timestamps sort lexically
                    .map(this::read)
                    .flatMap(java.util.Optional::stream)
                    .toList();
        } catch (IOException e) {
            throw new IllegalStateException("cannot list history dir: " + directory, e);
        }
    }

    private Path runFile(UpdateHistoryRunDto run) {
        return directory.resolve(run.timestamp() + ".json");
    }

    private java.util.Optional<UpdateHistoryRunDto> read(Path path) {
        try {
            return java.util.Optional.of(mapper.readValue(path.toFile(), UpdateHistoryRunDto.class));
        } catch (IOException e) {
            return java.util.Optional.empty(); // corrupt/partial file — skip rather than fail the whole view
        }
    }
}