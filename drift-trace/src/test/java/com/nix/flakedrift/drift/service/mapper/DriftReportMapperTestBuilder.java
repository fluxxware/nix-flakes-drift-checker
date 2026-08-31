package com.nix.flakedrift.drift.service.mapper;

import com.nix.flakedrift.drift.domain.model.DeploymentTarget;
import com.nix.flakedrift.drift.dto.DriftReportDto;

/** Builds a mapping result from a {@link DriftReportMapperTestData.Scenario}. */
public final class DriftReportMapperTestBuilder {
    private final DriftReportMapperTestData.Scenario scenario;

    public DriftReportMapperTestBuilder(DriftReportMapperTestData.Scenario scenario) {
        this.scenario = scenario;
    }

    public DriftReportDto build() {
        return new DriftReportMapper().assemble(
                scenario.tree(),
                scenario.live(),
                scenario.drift(),
                DeploymentTarget.local());
    }
}
