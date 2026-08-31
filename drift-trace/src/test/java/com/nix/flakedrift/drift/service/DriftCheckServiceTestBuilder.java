package com.nix.flakedrift.drift.service;

import com.nix.flakedrift.drift.api.IDriftCheckService;
import com.nix.flakedrift.drift.domain.model.DeploymentTarget;
import com.nix.flakedrift.drift.dto.DriftCheckRequest;
import com.nix.flakedrift.drift.dto.DriftReportDto;
import com.nix.flakedrift.drift.service.impl.DriftCheckServiceImpl;
import com.nix.flakedrift.drift.service.impl.DriftCompareServiceImpl;
import com.nix.flakedrift.drift.service.mapper.DriftReportMapper;

import static com.nix.flakedrift.drift.service.DriftCheckServiceTestData.ROOT_FLAKE_PATH;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Builds a fully wired drift-check service for a {@link DriftCheckServiceTestData.Scenario}.
 * The scenario is a required dependency (constructor); each external service mock is wired
 * independently via its {@code withSetup...Mock()} step, and {@link #build()} runs the pipeline.
 */
public final class DriftCheckServiceTestBuilder {
    private final IWorkspaceGraphService treeService = mock(IWorkspaceGraphService.class);
    private final ILiveStateService liveService = mock(ILiveStateService.class);
    private final DriftCheckServiceTestData.Scenario scenario;

    public DriftCheckServiceTestBuilder(DriftCheckServiceTestData.Scenario scenario) {
        this.scenario = scenario;
    }

    public DriftCheckServiceTestBuilder withSetupWorkspaceGraphMock() {
        when(treeService.buildDependencyGraph(ROOT_FLAKE_PATH)).thenReturn(scenario.tree());
        return this;
    }

    public DriftCheckServiceTestBuilder withSetupLiveStateMock() {
        when(liveService.probe(scenario.tree())).thenReturn(scenario.live());
        return this;
    }

    public DriftReportDto build() {
        IDriftCheckService service = new DriftCheckServiceImpl(
                treeService,
                liveService,
                new DriftCompareServiceImpl(),
                new DriftReportMapper());
        return service.checkForDrift(new DriftCheckRequest(ROOT_FLAKE_PATH, DeploymentTarget.local()));
    }
}
