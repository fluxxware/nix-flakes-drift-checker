package com.nix.flakedrift.drift.service.mapper;

import com.nix.flakedrift.drift.domain.model.DriftType;
import com.nix.flakedrift.drift.dto.DriftReportDto;
import com.nix.flakedrift.drift.dto.FlakeGraphNodeDto;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.nix.flakedrift.drift.service.mapper.DriftReportMapperTestData.childMissingFromLive;
import static com.nix.flakedrift.drift.service.mapper.DriftReportMapperTestData.wiredMachine;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Atomic behavior tests for {@link DriftReportMapper}, one assertion area per test. */
class DriftReportMapperTests {


    @Test
    void givenDriftedApp_whenAssembling_thenAppMarkedLocalDriftAndChainCause() {
        DriftReportDto report = new DriftReportMapperTestBuilder(wiredMachine())
                .build();

        FlakeGraphNodeDto appDto = report.root.children.get(1);
        assertEquals(2, appDto.driftTypes.size());
        assertTrue(appDto.driftTypes.contains(DriftType.LOCAL_DRIFT));
        assertTrue(appDto.driftTypes.contains(DriftType.CHAIN_STALE_CAUSE));
    }

    @Test
    void givenDriftedApp_whenAssembling_thenAppNotLive() {
        DriftReportDto report = new DriftReportMapperTestBuilder(wiredMachine())
                .build();

        FlakeGraphNodeDto appDto = report.root.children.get(1);
        assertFalse(appDto.livePresent);
    }

    @Test
    void givenAppMissingFromStore_whenAssembling_thenRootReportsItAsUnrefreshedMember() {
        DriftReportDto report = new DriftReportMapperTestBuilder(wiredMachine())
                .build();

        assertEquals(List.of("app"), report.root.unrefreshedMembers.stream().map(m -> m.name()).toList());
    }

    @Test
    void givenMachineRootTransitivelyStale_whenAssembling_thenRootMarkedTransitive() {
        DriftReportDto report = new DriftReportMapperTestBuilder(wiredMachine())
                .build();

        assertEquals(1, report.root.driftTypes.size());
        assertTrue(report.root.driftTypes.contains(DriftType.CHAIN_STALE_TRANSITIVE));
        assertTrue(report.root.livePresent);
    }

    @Test
    void givenRemoteUpstream_whenAssembling_thenMarkedRemote() {
        DriftReportDto report = new DriftReportMapperTestBuilder(wiredMachine())
                .build();

        FlakeGraphNodeDto upstreamDto = report.root.children.get(0);
        assertEquals(List.of(DriftType.REMOTE), upstreamDto.driftTypes.stream().toList());
    }

    @Test
    void givenWiredMachine_whenAssembling_thenCountsReflectNodes() {
        DriftReportDto report = new DriftReportMapperTestBuilder(wiredMachine())
                .build();

        int expectedNodeCount = 3;
        int expectedInSync = 1;
        int expectedDrifted = 2;
        assertEquals(expectedNodeCount, report.total);
        assertEquals(expectedInSync, report.synced);
        assertEquals(expectedDrifted, report.drifted);
    }

    @Test
    void givenChildMissingFromLive_whenAssembling_thenDefaultsToNotPresent() {
        DriftReportDto report = new DriftReportMapperTestBuilder(childMissingFromLive())
                .build();

        assertFalse(report.root.children.get(0).livePresent);
    }
}
