package com.nix.flakedrift.drift.service.impl;

import com.nix.flakedrift.drift.domain.model.FlakeGraphNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

import static com.nix.flakedrift.drift.service.impl.LiveStateServiceTestData.mixedTree;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Atomic behavior tests for {@link LiveStateServiceImpl}, one outcome per test. */
class LiveStateServiceImplTests {


    @Test
    void givenLockHashRealizedInStore_whenProbing_thenPresent(@TempDir Path storeRoot) throws IOException {
        Map<FlakeGraphNode, Boolean> live = new LiveStateServiceTestBuilder(mixedTree())
                .withStoreRoot(storeRoot)
                .build();

        assertTrue(nodeByName(live, "app"), "lock hash realized in store");
    }

    @Test
    void givenLockHashNotInStore_whenProbing_thenNotPresent(@TempDir Path storeRoot) throws IOException {
        Map<FlakeGraphNode, Boolean> live = new LiveStateServiceTestBuilder(mixedTree())
                .withStoreRoot(storeRoot)
                .build();

        assertFalse(nodeByName(live, "undelivered"), "lock hash not realized in store");
    }

    @Test
    void givenNoLockOnNode_whenProbing_thenFallsBackToDiskHash(@TempDir Path storeRoot) throws IOException {
        Map<FlakeGraphNode, Boolean> live = new LiveStateServiceTestBuilder(mixedTree())
                .withStoreRoot(storeRoot)
                .build();

        assertTrue(nodeByName(live, "fallback"), "no lock -> falls back to realized disk hash");
    }

    @Test
    void givenRemoteNode_whenProbing_thenSkipped(@TempDir Path storeRoot) throws IOException {
        Map<FlakeGraphNode, Boolean> live = new LiveStateServiceTestBuilder(mixedTree())
                .withStoreRoot(storeRoot)
                .build();

        boolean anyRemoteKey = live.keySet().stream().anyMatch(n -> n.getPath() == null);
        assertFalse(anyRemoteKey, "remote nodes are not included in the live map");
    }

    private static boolean nodeByName(Map<FlakeGraphNode, Boolean> live, String name) {
        return live.entrySet().stream()
                .filter(e -> e.getKey().getName().equals(name))
                .findFirst()
                .map(Map.Entry::getValue)
                .orElseThrow(() -> new AssertionError("no node named '" + name + "'"));
    }
}
