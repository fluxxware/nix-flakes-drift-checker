package com.nix.flakedrift.app.cli;

import com.nix.flakedrift.app.di.ServiceRegistry;
import com.nix.flakedrift.app.history.HistoryStore;
import com.nix.flakedrift.drift.dto.UpdateHistoryRunDto;
import com.nix.flakedrift.drift.dto.UpdateResultDto;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Callable;

@Command(name = "history", mixinStandardHelpOptions = true, description = "Show the update-history audit log.")
public final class HistoryCommand implements Callable<Integer> {
    @Option(names = "--last", defaultValue = "10", description = "Show the last N runs (default: ${DEFAULT-VALUE}).")
    private int last;

    @Option(names = "--flake", description = "Only show runs touching this flake name.")
    private String flake;

    @Option(names = "--history", description = "Override the update-history directory.")
    private Path historyOverride;

    @Option(names = "--json", description = "Dump the raw history JSON.")
    private boolean json;

    @Override
    public Integer call() {
        ServiceRegistry registry = new ServiceRegistry();
        Path historyDir = historyOverride != null
                ? historyOverride
                : registry.configuration().history().historyDirectory();
        HistoryStore store = new HistoryStore(historyDir);

        List<UpdateHistoryRunDto> runs = store.readAll();
        if (json) {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper()
                    .enable(com.fasterxml.jackson.databind.SerializationFeature.INDENT_OUTPUT);
            try {
                System.out.println(mapper.writeValueAsString(runs));
            } catch (Exception e) {
                throw new IllegalStateException("cannot render history JSON", e);
            }
            return 0;
        }

        if (runs.isEmpty()) {
            System.out.println("[HISTORY] No update runs recorded in " + historyDir + ".");
            return 0;
        }

        var filtered = runs.stream()
                .filter(r -> flake == null || r.flakes().stream().anyMatch(f -> f.name().equals(flake)))
                .toList();
        var shown = filtered.stream().skip(Math.max(0, filtered.size() - last)).toList();

        for (int i = shown.size() - 1; i >= 0; i--) {
            render(shown.get(i));
        }
        return 0;
    }

    private static void render(UpdateHistoryRunDto run) {
        System.out.println();
        System.out.println("⟐ " + run.timestamp() + "  tool=" + run.toolVersion() + "  root=" + run.root());
        for (UpdateResultDto flake : run.flakes()) {
            System.out.printf("  [%s] d%d  %-12s %s → %s%n",
                    flake.changed() ? "\u2713" : "=",
                    flake.depth(),
                    flake.name(),
                    shortHash(flake.lockBefore()),
                    shortHash(flake.lockAfter()));
        }
        System.out.printf("  summary: %s total, %s changed, %s unchanged%n",
                run.summary().total(), run.summary().changed(), run.summary().unchanged());
    }

    private static String shortHash(String sha) {
        return sha == null ? "(none)" : sha.length() > 16 ? sha.substring(0, 16) + "…" : sha;
    }
}