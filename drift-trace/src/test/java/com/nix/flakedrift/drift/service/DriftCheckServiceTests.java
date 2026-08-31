package com.nix.flakedrift.drift.service;

import com.nix.flakedrift.drift.domain.model.DriftType;
import com.nix.flakedrift.drift.dto.DriftReportDto;
import com.nix.flakedrift.drift.dto.FlakeGraphNodeDto;
import com.nix.flakedrift.drift.service.impl.DriftCheckServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

import static com.nix.flakedrift.drift.service.DriftCheckServiceTestData.Totals;
import static com.nix.flakedrift.drift.service.DriftCheckServiceTestData.cleanTree;
import static com.nix.flakedrift.drift.service.DriftCheckServiceTestData.cleanTreeWithRemote;
import static com.nix.flakedrift.drift.service.DriftCheckServiceTestData.deepStaleChain;
import static com.nix.flakedrift.drift.service.DriftCheckServiceTestData.driftedLeafUnderHub;
import static com.nix.flakedrift.drift.service.DriftCheckServiceTestData.undeployedApp;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Behavior tests of the drift-check pipeline. Each test drives the real
 * {@link DriftCheckServiceImpl} (real classifier + mapper) and mocks only the
 * external inputs. Fixtures and expected counts come from
 * {@link DriftCheckServiceTestData}.
 */
@ExtendWith(MockitoExtension.class)
class DriftCheckServiceTests {

    @Test
    void givenCleanTree_whenCheckingDrift_thenNothingIsDrifted() {
        DriftReportDto report = new DriftCheckServiceTestBuilder(cleanTree())
                .withSetupWorkspaceGraphMock()
                .withSetupLiveStateMock()
                .build();

        Totals expected = cleanTree().totals();
        assertEquals(expected.total(), report.total);
        assertEquals(expected.synced(), report.synced);
        assertEquals(expected.drifted(), report.drifted);
    }

    @Test
    void givenRemoteLeafOnly_whenCheckingDrift_thenLeafIsNotDrifted() {
        DriftReportDto report = new DriftCheckServiceTestBuilder(cleanTreeWithRemote())
                .withSetupWorkspaceGraphMock()
                .withSetupLiveStateMock()
                .build();

        FlakeGraphNodeDto remoteUpstreamDto = report.root.children.get(0);
        assertEquals(Set.of(DriftType.REMOTE), remoteUpstreamDto.driftTypes);

        Totals expected = cleanTreeWithRemote().totals();
        assertEquals(expected.total(), report.total);
        assertEquals(expected.synced(), report.synced);
        assertEquals(expected.drifted(), report.drifted);
    }

    @Test
    void givenDriftedLeaf_whenCheckingDrift_thenLeafIsCause_andAncestorsTransitive() {
        DriftReportDto report = new DriftCheckServiceTestBuilder(driftedLeafUnderHub())
                .withSetupWorkspaceGraphMock()
                .withSetupLiveStateMock()
                .build();

        FlakeGraphNodeDto leafDto = report.root.children.get(0).children.get(0);
        assertTrue(leafDto.driftTypes.contains(DriftType.LOCAL_DRIFT));
        assertTrue(leafDto.driftTypes.contains(DriftType.CHAIN_STALE_CAUSE));

        FlakeGraphNodeDto hubDto = report.root.children.get(0);
        assertEquals(Set.of(DriftType.CHAIN_STALE_TRANSITIVE), hubDto.driftTypes);
        String expectedUnrefreshedLeaf = "leaf-app";
        assertEquals(List.of(expectedUnrefreshedLeaf), hubDto.unrefreshedMembers.stream().map(m -> m.name()).toList());

        assertEquals(Set.of(DriftType.CHAIN_STALE_TRANSITIVE), report.root.driftTypes);

        Totals expected = driftedLeafUnderHub().totals();
        assertEquals(expected.total(), report.total);
        assertEquals(expected.synced(), report.synced);
        assertEquals(expected.drifted(), report.drifted);
    }

    @Test
    void givenLockedAppMissingFromStore_whenCheckingDrift_thenAppIsUndeployed() {
        DriftReportDto report = new DriftCheckServiceTestBuilder(undeployedApp())
                .withSetupWorkspaceGraphMock()
                .withSetupLiveStateMock()
                .build();

        FlakeGraphNodeDto undeployedAppDto = report.root.children.get(0);
        assertTrue(undeployedAppDto.driftTypes.contains(DriftType.UNDEPLOYED));
        assertFalse(undeployedAppDto.livePresent);

        Totals expected = undeployedApp().totals();
        assertEquals(expected.total(), report.total);
        assertEquals(expected.synced(), report.synced);
        assertEquals(expected.drifted(), report.drifted);
    }

    @Test
    void givenDeepStaleChain_whenCheckingDrift_thenPropagatesToRoot() {
        DriftReportDto report = new DriftCheckServiceTestBuilder(deepStaleChain())
                .withSetupWorkspaceGraphMock()
                .withSetupLiveStateMock()
                .build();

        assertEquals(Set.of(DriftType.CHAIN_STALE_TRANSITIVE), report.root.driftTypes);
        String expectedDirectChild = "c";
        assertEquals(List.of(expectedDirectChild), report.root.unrefreshedMembers.stream().map(m -> m.name()).toList());

        Totals expected = deepStaleChain().totals();
        assertEquals(expected.total(), report.total);
        assertEquals(expected.synced(), report.synced);
        assertEquals(expected.drifted(), report.drifted);
    }
}
