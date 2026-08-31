package com.nix.flakedrift.drift.service;

import com.nix.flakedrift.drift.domain.model.DriftType;
import com.nix.flakedrift.drift.domain.model.FlakeDependencyGraph;
import com.nix.flakedrift.drift.domain.model.FlakeGraphNode;
import com.nix.flakedrift.drift.service.impl.DriftCompareServiceImpl;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DriftCompareServiceImplTest {

    private final DriftCompareServiceImpl compare = new DriftCompareServiceImpl();

    @Test
    void givenMatchedDiskLockAndLive_whenClassifying_thenSync() {
        String diskHash = "sha256-a";
        String lockHash = "sha256-a";

        assertEquals(Set.of(), compare.classify(node(diskHash, lockHash), List.of(), true));
    }

    @Test
    void givenLockMissing_whenClassifying_thenNarHashAbsent() {
        String diskHash = "sha256-a";

        assertEquals(Set.of(DriftType.NARHASH_ABSENT), compare.classify(node(diskHash, null), List.of(), true));
        assertEquals(Set.of(DriftType.NARHASH_ABSENT), compare.classify(node(null, null), List.of(), false));
    }

    @Test
    void givenDiskDiffersFromLock_whenClassifying_thenLocalDriftAndChainCause() {
        String diskHash = "sha256-a";
        String staleLockHash = "sha256-b";

        assertEquals(Set.of(DriftType.LOCAL_DRIFT, DriftType.CHAIN_STALE_CAUSE),
                compare.classify(node(diskHash, staleLockHash), List.of(), true));
        assertEquals(Set.of(DriftType.LOCAL_DRIFT, DriftType.CHAIN_STALE_CAUSE),
                compare.classify(node(diskHash, staleLockHash), List.of(), false));
    }

    @Test
    void givenMatchButNotPresent_whenClassifying_thenUndeployed() {
        String hash = "sha256-a";

        assertEquals(Set.of(DriftType.UNDEPLOYED), compare.classify(node(hash, hash), List.of(), false));
    }

    @Test
    void givenNullPath_whenClassifying_thenRemote() {
        assertEquals(Set.of(DriftType.REMOTE), compare.classify(new FlakeGraphNode("remote", null), List.of(), true));
    }

    @Test
    void givenChildIsCause_whenClassifying_thenTransitive() {
        String parentHash = "sha256-p";
        Set<DriftType> childCauseTypes = Set.of(DriftType.LOCAL_DRIFT, DriftType.CHAIN_STALE_CAUSE);

        assertEquals(Set.of(DriftType.CHAIN_STALE_TRANSITIVE),
                compare.classify(node(parentHash, parentHash),
                        List.of(childCauseTypes), true));
    }

    @Test
    void givenWholeChainWithCause_whenClassifying_thenAllTransitive() {
        Set<DriftType> leafTypes = Set.of(DriftType.LOCAL_DRIFT, DriftType.CHAIN_STALE_CAUSE);

        Set<DriftType> midTypes = compare.classify(node("sha256-m", "sha256-m"), List.of(leafTypes), true);
        assertEquals(Set.of(DriftType.CHAIN_STALE_TRANSITIVE), midTypes);

        Set<DriftType> topTypes = compare.classify(node("sha256-t", "sha256-t"), List.of(midTypes), true);
        assertEquals(Set.of(DriftType.CHAIN_STALE_TRANSITIVE), topTypes);

        Set<DriftType> rootTypes = compare.classify(node("sha256-r", "sha256-r"), List.of(topTypes), true);
        assertEquals(Set.of(DriftType.CHAIN_STALE_TRANSITIVE), rootTypes);
    }

    @Test
    void givenNodeWithTransitiveChildAndAbsentOwnHash_whenClassifying_thenBothMarked() {
        String rootHash = "sha256-r";
        Set<DriftType> childTransitiveTypes = Set.of(DriftType.CHAIN_STALE_TRANSITIVE);

        assertEquals(Set.of(DriftType.NARHASH_ABSENT, DriftType.CHAIN_STALE_TRANSITIVE),
                compare.classify(node(rootHash, null), List.of(childTransitiveTypes), true));
    }

    @Test
    void givenTree_whenEvaluating_thenChildrenComputedFirst() {
        FlakeGraphNode leaf = node("sha256-a", "sha256-b");
        FlakeGraphNode parent = node("sha256-p", "sha256-p");
        parent.addChild(leaf);
        FlakeDependencyGraph tree = new FlakeDependencyGraph(parent);

        Map<FlakeGraphNode, Set<DriftType>> drift =
                compare.evaluate(tree, Map.of(parent, true, leaf, true));

        assertEquals(Set.of(DriftType.LOCAL_DRIFT, DriftType.CHAIN_STALE_CAUSE), drift.get(leaf));
        assertEquals(Set.of(DriftType.CHAIN_STALE_TRANSITIVE), drift.get(parent));
    }

    private static FlakeGraphNode node(String disk, String lock) {
        FlakeGraphNode n = new FlakeGraphNode("node", Path.of("/tmp/test-flake"));
        n.setDiskHash(disk);
        n.setLockHash(lock);
        return n;
    }
}
