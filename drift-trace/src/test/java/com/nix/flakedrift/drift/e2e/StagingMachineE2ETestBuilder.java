package com.nix.flakedrift.drift.e2e; // або у відповідному пакеті

import com.nix.flakedrift.drift.infra.WorkspaceGraphServiceTestData;

import com.nix.flakedrift.drift.infra.impl.*;
import com.nix.flakedrift.drift.service.impl.*;
import com.nix.flakedrift.drift.service.mapper.DriftReportMapper;

import com.nix.flakedrift.drift.domain.model.DeploymentTarget;
import com.nix.flakedrift.drift.api.IDriftCheckService;
import com.nix.flakedrift.drift.dto.DriftCheckRequest;

import java.net.URISyntaxException;
import java.nio.file.Path;

public final class StagingMachineE2ETestBuilder {
    private String fixturePath;

    public StagingMachineE2ETestBuilder withFixture(String fixturePath) {
        this.fixturePath = fixturePath;
        return this;
    }

    public IDriftCheckService build()throws URISyntaxException {
        Path rootDir = Path.of(getClass().getClassLoader().getResource(fixturePath).toURI());
        Path storeRoot = rootDir.resolve("nix/store");

        return  new DriftCheckServiceImpl(
                new WorkspaceGraphServiceImpl(new FlakeDefinitionService(), new LockFileService(), new NixCommandService()),
                new LiveStateServiceImpl(new NixCommandService(), storeRoot, true),
                new DriftCompareServiceImpl(),
                new DriftReportMapper()
        );
    }
}