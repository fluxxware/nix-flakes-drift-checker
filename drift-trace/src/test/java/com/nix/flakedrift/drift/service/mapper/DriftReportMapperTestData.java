package com.nix.flakedrift.drift.service.mapper;

import com.nix.flakedrift.drift.domain.model.DriftType;
import com.nix.flakedrift.drift.domain.model.FlakeDependencyGraph;
import com.nix.flakedrift.drift.domain.model.FlakeGraphNode;

import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

/**
 * Shared fixture data for {@link DriftReportMapperTests}: named scenario trees
 * with their live maps, drift maps and expected counts.
 */
public final class DriftReportMapperTestData {
    private DriftReportMapperTestData() {
    }

    private static final String UPSTREAM_NAME = "nixpkgs";
    private static final String DRIFTED_APP_NAME = "app";
    private static final Path MACHINE_ROOT_PATH = Path.of("/x");
    private static final Path DRIFTED_APP_PATH = Path.of("/x/app");

    public record Totals(int total, int synced, int drifted) {
    }

    /** A fully described mapping scenario: tree, live, drift, expected totals. */
    public record Scenario(FlakeDependencyGraph tree,
                           Map<FlakeGraphNode, Boolean> live,
                           Map<FlakeGraphNode, Set<DriftType>> drift,
                           Totals totals) {
    }

    /** Scenario: machine root + remote upstream + locally drifted app; root deployed, app not. */
    public static Scenario wiredMachine() {
        FlakeGraphNode remoteUpstream = new FlakeGraphNode(UPSTREAM_NAME, null);
        FlakeGraphNode driftedApp = new FlakeGraphNode(DRIFTED_APP_NAME, DRIFTED_APP_PATH);
        driftedApp.setDiskHash("sha256-edited-on-disk");
        driftedApp.setLockHash("sha256-stale-in-lock");
        FlakeGraphNode machineRoot = new FlakeGraphNode("machine-root", MACHINE_ROOT_PATH);
        machineRoot.addChild(remoteUpstream);
        machineRoot.addChild(driftedApp);

        Map<FlakeGraphNode, Boolean> live = Map.of(machineRoot, true, driftedApp, false);
        Map<FlakeGraphNode, Set<DriftType>> drift = Map.of(
                machineRoot, Set.of(DriftType.CHAIN_STALE_TRANSITIVE),
                remoteUpstream, Set.of(DriftType.REMOTE),
                driftedApp, Set.of(DriftType.LOCAL_DRIFT, DriftType.CHAIN_STALE_CAUSE));

        return new Scenario(new FlakeDependencyGraph(machineRoot), live, drift, new Totals(3, 1, 2));
    }

    /** Scenario: root and a synced child, but the live map only knows the root. */
    public static Scenario childMissingFromLive() {
        FlakeGraphNode machineRoot = new FlakeGraphNode("machine-root", Path.of("/x"));
        FlakeGraphNode syncedChild = new FlakeGraphNode("child", Path.of("/x/child"));
        syncedChild.setDiskHash("sha256-d");
        syncedChild.setLockHash("sha256-d");
        machineRoot.addChild(syncedChild);

        Map<FlakeGraphNode, Set<DriftType>> drift =
                Map.of(machineRoot, Set.of(), syncedChild, Set.of());

        return new Scenario(
                new FlakeDependencyGraph(machineRoot),
                Map.of(machineRoot, true),
                drift,
                new Totals(2, 2, 0));
    }
}
