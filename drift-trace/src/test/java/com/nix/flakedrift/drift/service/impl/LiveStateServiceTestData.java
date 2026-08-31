package com.nix.flakedrift.drift.service.impl;

import com.nix.flakedrift.drift.domain.model.FlakeDependencyGraph;
import com.nix.flakedrift.drift.domain.model.FlakeGraphNode;

import java.nio.file.Path;
import java.util.Map;

/**
 * Shared fixture data for {@link LiveStateServiceImplTests}: store objects and
 * node trees with their expected live-presence outcomes.
 */
public final class LiveStateServiceTestData {
    private LiveStateServiceTestData() {
    }

    public static final String REALIZED_HASH = "sha256-AAA";
    public static final String UNREALIZED_HASH = "sha256-BBB";
    public static final String FALLBACK_DISK_HASH = "sha256-CCC";

    /** A store object whose basename matches a node's disk path. */
    public record StoreObject(String name, String narHash) {
        Path in(Path storeRoot) {
            return storeRoot.resolve(name);
        }
    }

    /** Fully described tree: the graph, plus expected per-node presence. */
    public record TreeScenario(FlakeDependencyGraph graph,
                               Map<String, Boolean> expectedLive,
                               boolean expectsRemoteSkipped) {
    }

    /**
     * Tree: a deployed app (lock hash realized), an undelivered app (lock hash not
     * realized), a fallback app (no lock, disk hash realized) and a remote input.
     */
    public static TreeScenario mixedTree() {
        FlakeGraphNode deployedApp = node("app", "/x/app", "sha256-X", REALIZED_HASH);
        FlakeGraphNode undeliveredApp = node("undelivered", "/x/undelivered", "sha256-Y", UNREALIZED_HASH);
        // Disk-path basename must be exactly 'n3' so the store matcher finds '{...}-n3'.
        FlakeGraphNode fallbackApp = node("fallback", "/x/n3", FALLBACK_DISK_HASH, null);
        FlakeGraphNode remoteUpstream = new FlakeGraphNode("nixpkgs", null);
        FlakeGraphNode machineRoot = node("root", "/x", "sha256-R", "sha256-R");
        machineRoot.addChild(deployedApp);
        machineRoot.addChild(undeliveredApp);
        machineRoot.addChild(fallbackApp);
        machineRoot.addChild(remoteUpstream);

        Map<String, Boolean> expected = Map.of(
                "app", true,
                "undelivered", false,
                "fallback", true);

        return new TreeScenario(new FlakeDependencyGraph(machineRoot), expected, true);
    }

    private static FlakeGraphNode node(String name, String path, String disk, String lock) {
        FlakeGraphNode n = new FlakeGraphNode(name, Path.of(path));
        n.setDiskHash(disk);
        n.setLockHash(lock);
        return n;
    }
}
