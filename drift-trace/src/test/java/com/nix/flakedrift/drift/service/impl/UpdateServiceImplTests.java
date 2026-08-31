package com.nix.flakedrift.drift.service.impl;

import com.nix.flakedrift.drift.dto.UpdateCandidateDto;
import com.nix.flakedrift.drift.dto.UpdateResultDto;
import com.nix.flakedrift.drift.infra.INixCommandService;
import com.nix.flakedrift.drift.infra.WorkspaceGraphServiceTestBuilder;
import com.nix.flakedrift.drift.service.IUpdateService;
import com.nix.flakedrift.drift.service.IWorkspaceGraphService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static com.nix.flakedrift.drift.infra.WorkspaceGraphServiceTestData.DEEP_CHAIN_FIXTURE;
import static com.nix.flakedrift.drift.service.impl.UpdateServiceImplTestData.SYM_ROOT;
import static com.nix.flakedrift.drift.service.impl.UpdateServiceImplTestData.driftedLeafUnderSyncedParent;
import static com.nix.flakedrift.drift.service.impl.UpdateServiceImplTestData.localUpdateTree;
import static com.nix.flakedrift.drift.service.impl.UpdateServiceImplTestData.syncedTree;
import static com.nix.flakedrift.drift.service.impl.UpdateServiceImplTestData.twoDriftedSiblingsAtSameDepth;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.inOrder;

/** (no real nix, no store) coverage of the update chain ordering + update execution. */
class UpdateServiceImplTests {

    @Test
    void givenRealFixtures_whenCandidates_thenDeepestFirstOrder() throws Exception {
        Path rootDir = Path.of(getClass().getClassLoader().getResource(DEEP_CHAIN_FIXTURE).toURI());
        IWorkspaceGraphService graphService = new WorkspaceGraphServiceTestBuilder()
                .withSetupNixCommandMock()
                .build();
        IUpdateService service = new UpdateServiceImpl(graphService, new DriftCompareServiceImpl(),
                org.mockito.Mockito.mock(INixCommandService.class));

        List<UpdateCandidateDto> chain = service.findUpdateCandidates(rootDir);

        assertEquals(
                List.of("leaf-flake-1", "layer-flake-3", "layer-flake-2", "layer-flake-1", "root"),
                names(chain));
        assertEquals(List.of(4, 3, 2, 1, 0), depths(chain));
        assertTrue(chain.stream().allMatch(c -> c.path() != null),
                "only local flakes can be update candidates");
    }

    @Test
    void givenTwoDriftedSiblings_whenCandidates_thenNameTiebreakWithinDepth() {
        IUpdateService service = new UpdateServiceImplTestBuilder(twoDriftedSiblingsAtSameDepth())
                .withSetupWorkspaceGraphMock()
                .build();

        List<UpdateCandidateDto> chain = service.findUpdateCandidates(SYM_ROOT);

        assertEquals(List.of("a-module", "z-module", "root"), names(chain));
        assertEquals(List.of(1, 1, 0), depths(chain));
    }

    @Test
    void givenRealLockFixtures_whenUpdateAll_thenRunsNixFlakeUpdateDeepestFirst(@TempDir Path tmp) {
        UpdateServiceImplTestBuilder testBuilder = new UpdateServiceImplTestBuilder(localUpdateTree(tmp))
                .withSetupWorkspaceGraphMock()
                .withMaterializedLocks();
        IUpdateService service = testBuilder.build();

        service.updateAll(tmp);

        var order = inOrder(testBuilder.nix());
        order.verify(testBuilder.nix()).run(List.of("nix", "flake", "update", "--flake", tmp.resolve("agg/leaf").toString()));
        order.verify(testBuilder.nix()).run(List.of("nix", "flake", "update", "--flake", tmp.resolve("agg").toString()));
        order.verify(testBuilder.nix()).run(List.of("nix", "flake", "update", "--flake", tmp.toString()));
        order.verifyNoMoreInteractions();
    }

    @Test
    void givenRealLockFiles_whenUpdateAll_thenDeepestFirstOrder(@TempDir Path tmp) {
        IUpdateService service = new UpdateServiceImplTestBuilder(localUpdateTree(tmp))
                .withSetupWorkspaceGraphMock()
                .withMaterializedLocks()
                .build();

        List<UpdateResultDto> results = service.updateAll(tmp);

        assertEquals(List.of("leaf", "agg", "root"), results.stream().map(UpdateResultDto::name).toList());
        assertEquals(List.of(2, 1, 0), results.stream().map(UpdateResultDto::depth).toList());
    }

    @Test
    void givenRealLockFiles_whenUpdateAll_thenBeforeFingerprintsRecorded(@TempDir Path tmp) {
        IUpdateService service = new UpdateServiceImplTestBuilder(localUpdateTree(tmp))
                .withSetupWorkspaceGraphMock()
                .withMaterializedLocks()
                .build();

        List<UpdateResultDto> results = service.updateAll(tmp);

        for (UpdateResultDto result : results) {
            assertTrue(result.lockBefore() != null && result.lockBefore().startsWith("sha256-"),
                    "before fingerprint recorded: " + result.name());
        }
    }

    @Test
    void givenLocksUntouchedByNix_whenUpdateAll_thenMarkedUnchanged(@TempDir Path tmp) {
        IUpdateService service = new UpdateServiceImplTestBuilder(localUpdateTree(tmp))
                .withSetupWorkspaceGraphMock()
                .withMaterializedLocks()
                .build();

        List<UpdateResultDto> results = service.updateAll(tmp);

        for (UpdateResultDto result : results) {
            assertEquals(result.lockBefore(), result.lockAfter(),
                    "lock file untouched by mocked nix -> before == after");
            assertFalse(result.changed(), "no real nix write -> unchanged");
        }
    }

    @Test
    void givenFullySyncedTree_whenCandidates_thenEmpty() {
        IUpdateService service = new UpdateServiceImplTestBuilder(syncedTree())
                .withSetupWorkspaceGraphMock()
                .build();

        List<UpdateCandidateDto> chain = service.findUpdateCandidates(SYM_ROOT);

        assertTrue(chain.isEmpty(), "no drift -> no update candidates");
    }

    @Test
    void givenDriftedLeafOnly_whenCandidates_thenTransitiveAncestorsUpToRoot() {
        IUpdateService service = new UpdateServiceImplTestBuilder(driftedLeafUnderSyncedParent())
                .withSetupWorkspaceGraphMock()
                .build();

        List<UpdateCandidateDto> chain = service.findUpdateCandidates(SYM_ROOT);

        assertEquals(
                List.of("drifty", "clean", "root"),
                names(chain),
                "transitive ancestors up to root are in the chain, untouched siblings are not");
    }

    private static List<String> names(List<UpdateCandidateDto> chain) {
        return chain.stream().map(UpdateCandidateDto::name).toList();
    }

    private static List<Integer> depths(List<UpdateCandidateDto> chain) {
        return chain.stream().map(UpdateCandidateDto::depth).toList();
    }
}