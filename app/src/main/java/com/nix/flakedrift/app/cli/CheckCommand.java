package com.nix.flakedrift.app.cli;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nix.flakedrift.app.di.ServiceRegistry;
import com.nix.flakedrift.app.report.ReportRenderer;
import com.nix.flakedrift.drift.dto.DriftCheckRequest;
import com.nix.flakedrift.drift.dto.DriftReportDto;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.nio.file.Path;
import java.util.concurrent.Callable;

@Command(name = "check", mixinStandardHelpOptions = true, description = "Check the workspace against a live target.")
public final class CheckCommand implements Callable<Integer> {
    private final ObjectMapper mapper = new ObjectMapper();

    @Option(names = "--root", description = "Root flake directory to scan from (default: from config).")
    private Path root;

    @Option(names = "--mock-store", hidden = true,
            description = "Point the live probe at a mock store directory (simulates deployment).")
    private Path mockStore;

    @Option(names = "--json", description = "Emit a machine-readable JSON report.")
    private boolean json;

    @Override
    public Integer call() throws Exception {
        ServiceRegistry registry = mockStore == null
                ? new ServiceRegistry()
                : new ServiceRegistry(mockStore);
        Path scanRoot = root != null ? root : registry.configuration().workspace().defaultRoot();
        DriftReportDto report = registry.driftCheck().checkForDrift(new DriftCheckRequest(scanRoot));
        if (json) {
            System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(report));
        } else {
            System.out.println("[SCAN] Scanning workspace: " + scanRoot);
            if (mockStore != null) {
                System.out.println("  (live probe against mock store: " + mockStore + ")");
            }
            ReportRenderer.render(report);
        }
        return report.drifted == 0 ? 0 : 1;
    }
}
