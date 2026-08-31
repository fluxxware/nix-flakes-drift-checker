package com.nix.flakedrift.drift.service.impl;
import com.nix.flakedrift.drift.api.IDriftCheckService;

import com.nix.flakedrift.drift.domain.model.DriftType;
import com.nix.flakedrift.drift.domain.model.FlakeDependencyGraph;
import com.nix.flakedrift.drift.domain.model.FlakeGraphNode;
import com.nix.flakedrift.drift.dto.DriftCheckRequest;
import com.nix.flakedrift.drift.dto.DriftReportDto;
import com.nix.flakedrift.drift.service.IDriftCompareService;
import com.nix.flakedrift.drift.service.ILiveStateService;
import com.nix.flakedrift.drift.service.IWorkspaceGraphService;
import com.nix.flakedrift.drift.service.mapper.DriftReportMapper;

import java.util.Map;
import java.util.Set;

/** {@link IDriftCheckService} — flat pipeline: build tree, probe live, evaluate drift, assemble report. */
public final class DriftCheckServiceImpl implements IDriftCheckService {
    private final IWorkspaceGraphService workspaceGraphService;
    private final ILiveStateService liveStateService;
    private final IDriftCompareService driftCompareService;
    private final DriftReportMapper driftReportMapper;

    public DriftCheckServiceImpl(
            IWorkspaceGraphService workspaceGraphService,
            ILiveStateService liveStateService,
            IDriftCompareService driftCompareService,
            DriftReportMapper driftReportMapper) {
        this.workspaceGraphService = workspaceGraphService;
        this.liveStateService = liveStateService;
        this.driftCompareService = driftCompareService;
        this.driftReportMapper = driftReportMapper;
    }

    @Override
    public DriftReportDto checkForDrift(DriftCheckRequest driftCheckRequest) {
        FlakeDependencyGraph tree = workspaceGraphService.buildDependencyGraph(driftCheckRequest.flakeRootAbsolutePath());
        Map<FlakeGraphNode, Boolean> live = liveStateService.probe(tree);
        Map<FlakeGraphNode, Set<DriftType>> drift = driftCompareService.evaluate(tree, live);
        return driftReportMapper.assemble(tree, live, drift, driftCheckRequest.deploymentTarget());
    }
}