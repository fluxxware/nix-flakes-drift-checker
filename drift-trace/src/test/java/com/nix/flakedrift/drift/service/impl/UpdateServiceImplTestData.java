package com.nix.flakedrift.drift.service.impl;

import com.nix.flakedrift.drift.domain.model.FlakeDependencyGraph;
import com.nix.flakedrift.drift.domain.model.FlakeGraphNode;
import com.nix.flakedrift.drift.dto.UpdateCandidateDto;

import java.nio.file.Path;
import java.util.List;

/**
 * Shared fixture data for {@link UpdateServiceImplTests}: named scenario trees and
 * their expected update chains, deepest-first. Mirrors the Data/Builder pattern of
 * the rest of the codebase — the data lives here, tests stay declarative.
 */
public final class UpdateServiceImplTestData {
    private UpdateServiceImplTestData() {
    }

    /**
     * Symbolic root for injected node paths. NEVER exists on disk: the graph service
     * and the nix runner are mocked, so these paths are only compared/ordered by the
     * service (a local flake is a candidate iff path != null). Purely illustrative.
     */
    public static final Path SYM_ROOT = Path.of("/synthetic/flake-root");
    public static final String SYNC_HASH = "sha256-same";
    public static final String DRIFTED_DISK = "sha256-disk";
    public static final String DRIFTED_LOCK = "sha256-lock";

    /**
     * A fully described scenario: the tree to build and the expected candidate chain
     * (names, deepest level first).
     */
    public record Scenario(String name, FlakeDependencyGraph tree, List<String> expectedChain) {
    }

    /**
     * A tree rooted at a REAL directory on disk (unlike the {@link Scenario} trees,
     * which use the symbolic {@link #SYM_ROOT}). Needed by tests that write actual
     * {@code flake.lock} files and read their fingerprints back.
     */
    public record LocalFlakeTree(FlakeDependencyGraph graph, List<UpdateCandidateDto> updateChain) {
    }

    public static FlakeGraphNode node(String name, String childDir, int depth, String diskHash, String lockHash) {
        FlakeGraphNode n = new FlakeGraphNode(name, SYM_ROOT.resolve(childDir));
        n.setDepth(depth);
        n.setDiskHash(diskHash);
        n.setLockHash(lockHash);
        return n;
    }

    /**
     * The stacked {@code root → agg → leaf} chain, re-rooted onto {@code realRoot}
     * so the candidate flake.lock files can be created on disk. The update chain is
     * deepest-first (leaf depth 2, agg depth 1, root depth 0) — exactly the order
     * {@code updateAll} must follow.
     */
    public static LocalFlakeTree localUpdateTree(Path realRoot) {
        FlakeGraphNode root = realNode("root", "", realRoot, 0, SYNC_HASH, SYNC_HASH);
        FlakeGraphNode agg = realNode("agg", "agg", realRoot, 1, SYNC_HASH, SYNC_HASH);
        FlakeGraphNode leaf = realNode("leaf", "agg/leaf", realRoot, 2, DRIFTED_DISK, DRIFTED_LOCK);
        agg.addChild(leaf);
        root.addChild(agg);
        List<UpdateCandidateDto> updateChain = List.of(
                new UpdateCandidateDto("leaf", realRoot.resolve("agg/leaf"), 2),
                new UpdateCandidateDto("agg", realRoot.resolve("agg"), 1),
                new UpdateCandidateDto("root", realRoot, 0));
        return new LocalFlakeTree(new FlakeDependencyGraph(root), updateChain);
    }

    private static FlakeGraphNode realNode(String name, String relDir, Path realRoot, int depth, String diskHash, String lockHash) {
        FlakeGraphNode n = new FlakeGraphNode(name, realRoot.resolve(relDir));
        n.setDepth(depth);
        n.setDiskHash(diskHash);
        n.setLockHash(lockHash);
        return n;
    }

    // ---- scenario: two cause siblings on the same level -> deterministic name tiebreak ----
    public static Scenario twoDriftedSiblingsAtSameDepth() {
        FlakeGraphNode root = node("root", "", 0, SYNC_HASH, SYNC_HASH);
        FlakeGraphNode z = node("z-module", "z", 1, DRIFTED_DISK, DRIFTED_LOCK);
        FlakeGraphNode a = node("a-module", "a", 1, DRIFTED_DISK, DRIFTED_LOCK);
        root.addChild(z);
        root.addChild(a);
        return new Scenario("twoDriftedSiblingsAtSameDepth",
                new FlakeDependencyGraph(root),
                List.of("a-module", "z-module", "root"));
    }

    // ---- scenario: a drifted leaf deep in the tree -> deepest first, then ancestors ----
    public static Scenario driftedLeafChain() {
        FlakeGraphNode root = node("root", "", 0, SYNC_HASH, SYNC_HASH);
        FlakeGraphNode agg = node("agg", "agg", 1, SYNC_HASH, SYNC_HASH);
        FlakeGraphNode leaf = node("leaf", "agg/leaf", 2, DRIFTED_DISK, DRIFTED_LOCK);
        agg.addChild(leaf);
        root.addChild(agg);
        return new Scenario("driftedLeafChain",
                new FlakeDependencyGraph(root),
                List.of("leaf", "agg", "root"));
    }

    // ---- scenario: fully synced tree -> no candidates ----
    public static Scenario syncedTree() {
        FlakeGraphNode root = node("root", "", 0, SYNC_HASH, SYNC_HASH);
        FlakeGraphNode clean = node("clean", "clean", 1, SYNC_HASH, SYNC_HASH);
        root.addChild(clean);
        return new Scenario("syncedTree",
                new FlakeDependencyGraph(root),
                List.of());
    }

    // ---- scenario: drifted leaf under a synced parent -> parent becomes transitive ----
    public static Scenario driftedLeafUnderSyncedParent() {
        FlakeGraphNode root = node("root", "", 0, SYNC_HASH, SYNC_HASH);
        FlakeGraphNode clean = node("clean", "clean", 1, SYNC_HASH, SYNC_HASH);
        FlakeGraphNode drifty = node("drifty", "clean/drifty", 2, DRIFTED_DISK, DRIFTED_LOCK);
        clean.addChild(drifty);
        root.addChild(clean);
        return new Scenario("driftedLeafUnderSyncedParent",
                new FlakeDependencyGraph(root),
                List.of("drifty", "clean", "root"));
    }
}