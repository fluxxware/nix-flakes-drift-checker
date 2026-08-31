package com.nix.flakedrift.app.cli;

import com.nix.flakedrift.app.di.ServiceRegistry;
import com.nix.flakedrift.app.history.HistoryStore;
import com.nix.flakedrift.drift.dto.UpdateCandidateDto;
import com.nix.flakedrift.drift.dto.UpdateHistoryRunDto;
import com.nix.flakedrift.drift.dto.UpdateResultDto;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * {@code update} — update every stale flake lock in the chain,
 * deepest level first (nix itself cannot roll out a whole flake tree), and
 * append the run to the update-history audit log.
 */
@Command(name = "update", mixinStandardHelpOptions = true, description = "Update stale flake locks, deepest level first.")
public final class UpdateCommand implements Callable<Integer> {
    @Option(names = "--root", description = "Root flake directory to scan from (default: from config).")
    private Path root;

    @Option(names = "--dry-run", description = "Print the update chain without updating anything.")
    private boolean dryRun;

    @Option(names = "--history", description = "Override the update-history directory.")
    private Path historyOverride;

    @Override
    public Integer call() {
        ServiceRegistry registry = new ServiceRegistry();
        Path scanRoot = root != null ? root : registry.configuration().workspace().defaultRoot();

        List<UpdateCandidateDto> chain = registry.update().findUpdateCandidates(scanRoot);
        if (chain.isEmpty()) {
            System.out.println("[UPDATE] No stale flakes under " + scanRoot + " — nothing to do.");
            return 0;
        }
        System.out.println("[UPDATE] Update chain (" + chain.size() + " flakes, deepest first):");
        for (UpdateCandidateDto candidate : chain) {
            System.out.println("  d" + candidate.depth() + "  " + candidate.name() + "  " + candidate.path());
        }
        if (dryRun) {
            System.out.println("[UPDATE] Dry run — nothing updated.");
            return 0;
        }

        List<UpdateResultDto> results = registry.update().updateAll(scanRoot);
        for (UpdateResultDto result : results) {
            System.out.printf("[UPDATE] %-10s d%d  %s  %s → %s%n",
                    result.changed() ? "changed" : "same",
                    result.depth(),
                    result.name(),
                    shortHash(result.lockBefore()),
                    shortHash(result.lockAfter()));
        }
        System.out.println("[UPDATE] Done.");

        appendHistory(registry, scanRoot, results);
        return 0;
    }

    private void appendHistory(ServiceRegistry registry, Path scanRoot, List<UpdateResultDto> results) {
        Path historyDir = historyOverride != null
                ? historyOverride
                : registry.configuration().history().historyDirectory();
        UpdateHistoryRunDto run = UpdateHistoryRunDto.of(
                Instant.now().toString(),
                registry.configuration().version(),
                scanRoot.toString(),
                results);
        new HistoryStore(historyDir).append(run);
        System.out.println("[UPDATE] History appended to " + historyDir + "/" + run.timestamp() + ".json");
    }

    private static String shortHash(String sha) {
        return sha == null ? "(none)" : sha.length() > 16 ? sha.substring(0, 16) + "…" : sha;
    }
}