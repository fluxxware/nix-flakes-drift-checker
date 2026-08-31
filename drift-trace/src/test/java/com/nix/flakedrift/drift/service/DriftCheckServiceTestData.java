package com.nix.flakedrift.drift.service;

import com.nix.flakedrift.drift.domain.model.FlakeDependencyGraph;
import com.nix.flakedrift.drift.domain.model.FlakeGraphNode;

import java.nio.file.Path;

/**
 * Shared fixture data for {@link DriftCheckServiceTests}: named scenario trees,
 * their live maps and expected outcome counts. Mirrors the C# test-builder-base
 * pattern — the data lives here, tests stay declarative.
 */
public final class DriftCheckServiceTestData {
    private DriftCheckServiceTestData() {
    }

    public static final Path ROOT_FLAKE_PATH = Path.of("/machine");
    public static final String DISK_HASH = "sha256-disk";
    public static final String LOCK_HASH = "sha256-lock";
    public static final String ROOT_DISK_HASH = "sha256-root";

    /** Expected node totals for each scenario. */
    public record Totals(int total, int synced, int drifted) {
    }

    /** A fully described scenario: the tree to build, the live presence map, expected counts. */
    public record Scenario(String name, FlakeDependencyGraph tree,
                           java.util.Map<FlakeGraphNode, Boolean> live,
                           Totals totals) {
    }

    public static FlakeGraphNode pathNode(String name, String disk, String lock, Path path) {
        FlakeGraphNode n = new FlakeGraphNode(name, path);
        n.setDiskHash(disk);
        n.setLockHash(lock);
        return n;
    }

    public static FlakeGraphNode remote(String name) {
        return new FlakeGraphNode(name, null);
    }

    // ---- scenario: everything clean ----
    public static Scenario cleanTree() {
        FlakeGraphNode lib = pathNode("lib", DISK_HASH, DISK_HASH, ROOT_FLAKE_PATH.resolve("lib"));
        FlakeGraphNode root = new FlakeGraphNode("root", ROOT_FLAKE_PATH);
        root.setDiskHash(ROOT_DISK_HASH);
        root.setLockHash(ROOT_DISK_HASH);
        root.addChild(lib);
        return new Scenario("cleanTree",
                new FlakeDependencyGraph(root),
                java.util.Map.of(root, true, lib, true),
                new Totals(2, 2, 0));
    }

    // ---- scenario: remote leaf is not counted as drifted ----
    public static Scenario cleanTreeWithRemote() {
        FlakeGraphNode nixpkgs = remote("nixpkgs");
        FlakeGraphNode root = new FlakeGraphNode("root", ROOT_FLAKE_PATH);
        root.setDiskHash(ROOT_DISK_HASH);
        root.setLockHash(ROOT_DISK_HASH);
        root.addChild(nixpkgs);
        return new Scenario("cleanTreeWithRemote",
                new FlakeDependencyGraph(root),
                java.util.Map.of(root, true),
                new Totals(2, 2, 0));
    }

    // ---- scenario: a drifted leaf makes its parent (and root) transitive ----
    public static Scenario driftedLeafUnderHub() {
        FlakeGraphNode leafApp = pathNode("leaf-app", DISK_HASH, LOCK_HASH, ROOT_FLAKE_PATH.resolve("leaf-app"));
        FlakeGraphNode hub = pathNode("hub", DISK_HASH, DISK_HASH, ROOT_FLAKE_PATH.resolve("hub"));
        hub.addChild(leafApp);
        FlakeGraphNode root = new FlakeGraphNode("root", ROOT_FLAKE_PATH);
        root.setDiskHash(ROOT_DISK_HASH);
        root.setLockHash(ROOT_DISK_HASH);
        root.addChild(hub);
        return new Scenario("driftedLeafUnderHub",
                new FlakeDependencyGraph(root),
                java.util.Map.of(root, true, hub, true, leafApp, true),
                new Totals(3, 0, 3));
    }

    // ---- scenario: locked version is missing from the store (undeployed) ----
    public static Scenario undeployedApp() {
        FlakeGraphNode app = pathNode("app", DISK_HASH, DISK_HASH, ROOT_FLAKE_PATH.resolve("app"));
        FlakeGraphNode root = new FlakeGraphNode("root", ROOT_FLAKE_PATH);
        root.setDiskHash(ROOT_DISK_HASH);
        root.setLockHash(ROOT_DISK_HASH);
        root.addChild(app);
        // app is NOT present in the live store.
        return new Scenario("undeployedApp",
                new FlakeDependencyGraph(root),
                java.util.Map.of(root, true, app, false),
                new Totals(2, 1, 1));
    }

    // ---- scenario: a deep stale chain propagates to the root ----
    public static Scenario deepStaleChain() {
        FlakeGraphNode leafApp = pathNode("leaf-app", DISK_HASH, LOCK_HASH, ROOT_FLAKE_PATH.resolve("a"));
        FlakeGraphNode b = pathNode("b", DISK_HASH, DISK_HASH, ROOT_FLAKE_PATH.resolve("a/b"));
        b.addChild(leafApp);
        FlakeGraphNode c = pathNode("c", DISK_HASH, DISK_HASH, ROOT_FLAKE_PATH.resolve("a/b/c"));
        c.addChild(b);
        FlakeGraphNode root = new FlakeGraphNode("root", ROOT_FLAKE_PATH);
        root.setDiskHash(ROOT_DISK_HASH);
        root.setLockHash(ROOT_DISK_HASH);
        root.addChild(c);
        return new Scenario("deepStaleChain",
                new FlakeDependencyGraph(root),
                java.util.Map.of(root, true, c, true, b, true, leafApp, true),
                new Totals(4, 0, 4));
    }
}
