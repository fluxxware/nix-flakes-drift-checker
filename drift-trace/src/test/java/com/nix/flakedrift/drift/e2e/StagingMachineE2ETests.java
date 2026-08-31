package com.nix.flakedrift.drift.e2e;

import com.nix.flakedrift.drift.api.IDriftCheckService;
import com.nix.flakedrift.drift.domain.model.DeploymentTarget;
import com.nix.flakedrift.drift.domain.model.DriftType;
import com.nix.flakedrift.drift.dto.DriftCheckRequest;
import com.nix.flakedrift.drift.dto.DriftReportDto;
import com.nix.flakedrift.drift.dto.FlakeGraphNodeDto;
import com.nix.flakedrift.drift.infra.WorkspaceGraphServiceTestData;
import com.nix.flakedrift.drift.infra.impl.FlakeDefinitionService;
import com.nix.flakedrift.drift.infra.impl.LockFileService;
import com.nix.flakedrift.drift.infra.impl.NixCommandService;
import com.nix.flakedrift.drift.service.IWorkspaceGraphService;
import com.nix.flakedrift.drift.service.ILiveStateService;
import com.nix.flakedrift.drift.service.mapper.DriftReportMapper;
import com.nix.flakedrift.drift.service.impl.WorkspaceGraphServiceImpl;
import com.nix.flakedrift.drift.service.impl.LiveStateServiceImpl;
import com.nix.flakedrift.drift.service.impl.DriftCompareServiceImpl;
import com.nix.flakedrift.drift.service.impl.DriftCheckServiceImpl;
import org.junit.jupiter.api.Test;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * any changes in staging-machine/nix/store will fail this test
 */
class StagingMachineE2ETests {
    private static final String FIXTURE = "flakes/staging-machine";

    /**
     * 115 nodes: root + flake-01 + 56 modules + nested flake-03 + 56 remote nixpkgs.
     * 110 in sync: the 54 synced modules plus 56 remote nixpkgs inputs. 5 drifted:
     * root, flake-01, flake-02, flake-04, nested flake-03.
     * */
    @Test
    void givenMockStore_whenScanningStagingMachine_thenReportSummaryMatches() throws Exception {
        Path rootDir = Path.of(getClass().getClassLoader().getResource(FIXTURE).toURI());
        boolean liveAndMachineDeployed = true;

        IDriftCheckService checkService = new StagingMachineE2ETestBuilder()
                .withFixture(WorkspaceGraphServiceTestData.STAGING_FIXTURE)
                .build();
        DriftReportDto report = checkService.checkForDrift(new DriftCheckRequest(rootDir, DeploymentTarget.local()));

        assertEquals(115, report.total);
        assertEquals(110, report.synced);
        assertEquals(5, report.drifted);

        assertEquals(liveAndMachineDeployed, report.root.livePresent);
    }

    @Test
    void givenMockStore_whenScanningStagingMachine_thenChainDriftAndCausesAreAccurate() throws Exception {
        Path rootDir = Path.of(getClass().getClassLoader().getResource(FIXTURE).toURI());

        IDriftCheckService checkService = new StagingMachineE2ETestBuilder()
                .withFixture(WorkspaceGraphServiceTestData.STAGING_FIXTURE)
                .build();
        DriftReportDto report = checkService.checkForDrift(new DriftCheckRequest(rootDir, DeploymentTarget.local()));

        // flake-01 is the sole aggregator, deployed (mock store), stale over two causes.
        FlakeGraphNodeDto agg = report.root.children.get(0);
        assertEquals(true, agg.livePresent);
        assertEquals("flake-01", agg.name);
        assertEquals("[CHAIN_STALE_TRANSITIVE]", agg.driftTypes.toString());
        assertEquals("[flake-02, flake-04]",
                agg.unrefreshedMembers.stream().map(m -> m.name()).toList().toString());

        // 3 deliberate causes: flake-02, flake-04, nested flake-03.
        long causes = allNodes(report).stream()
                .filter(n -> n.driftTypes.contains(DriftType.CHAIN_STALE_CAUSE))
                .count();
        assertEquals(3, causes);

        // flake-04 is both cause and stale-transitive over its nested flake-03.
        FlakeGraphNodeDto f04 = find(report, "flake-04");
        assertTrue(f04.driftTypes.contains(DriftType.CHAIN_STALE_CAUSE));
        assertTrue(f04.driftTypes.contains(DriftType.CHAIN_STALE_TRANSITIVE));
    }

    private static java.util.List<FlakeGraphNodeDto> allNodes(DriftReportDto report) {
        java.util.List<FlakeGraphNodeDto> out = new java.util.ArrayList<>();
        collect(report.root, out);
        return out;
    }

    private static void collect(FlakeGraphNodeDto n, java.util.List<FlakeGraphNodeDto> out) {
        out.add(n);
        for (FlakeGraphNodeDto c : n.children) {
            collect(c, out);
        }
    }

    private static FlakeGraphNodeDto find(DriftReportDto report, String name) {
        return allNodes(report).stream().filter(n -> n.name.equals(name)).findFirst().orElseThrow();
    }
}
