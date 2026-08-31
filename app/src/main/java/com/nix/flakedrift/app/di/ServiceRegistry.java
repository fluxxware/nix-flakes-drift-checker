package com.nix.flakedrift.app.di;

import com.nix.flakedrift.drift.api.IDriftCheckService;
import com.nix.flakedrift.drift.configuration.AppConfiguration;
import com.nix.flakedrift.drift.infra.IFlakeDefinitionService;
import com.nix.flakedrift.drift.infra.ILockFileService;
import com.nix.flakedrift.drift.infra.INixCommandService;
import com.nix.flakedrift.drift.infra.impl.FlakeDefinitionService;
import com.nix.flakedrift.drift.infra.impl.LockFileService;
import com.nix.flakedrift.drift.infra.impl.NixCommandService;
import com.nix.flakedrift.drift.service.IDriftCompareService;
import com.nix.flakedrift.drift.service.ILiveStateService;
import com.nix.flakedrift.drift.service.IUpdateService;
import com.nix.flakedrift.drift.service.IWorkspaceGraphService;
import com.nix.flakedrift.drift.service.impl.DriftCheckServiceImpl;
import com.nix.flakedrift.drift.service.impl.DriftCompareServiceImpl;
import com.nix.flakedrift.drift.service.mapper.DriftReportMapper;
import com.nix.flakedrift.drift.service.impl.LiveStateServiceImpl;
import com.nix.flakedrift.drift.service.impl.UpdateServiceImpl;
import com.nix.flakedrift.drift.service.impl.WorkspaceGraphServiceImpl;

import java.nio.file.Path;

public final class ServiceRegistry {
    private final AppConfiguration configuration;
    private final INixCommandService nixCommandService;
    private final ILockFileService lockFileService;
    private final IFlakeDefinitionService flakeDefinitionService;

    private final ILiveStateService liveStateService;
    private final IWorkspaceGraphService workspaceGraphService;
    private final IDriftCompareService driftCompareService;
    private final DriftReportMapper driftReportMapper;
    private final IDriftCheckService driftCheckService;
    private final IUpdateService updateService;

    public ServiceRegistry() {
        this(new AppConfiguration());
    }

    /** @param storeRoot real {@code /nix/store} or a mock store directory to simulate deployment. */
    public ServiceRegistry(Path storeRoot) {
        this(new AppConfiguration().withStore(storeRoot, true));
    }

    private ServiceRegistry(AppConfiguration configuration) {
        this.configuration = configuration;
        this.nixCommandService = new NixCommandService();
        this.lockFileService = new LockFileService();
        this.flakeDefinitionService = new FlakeDefinitionService();

        this.liveStateService = new LiveStateServiceImpl(
                nixCommandService, configuration.store().root(), configuration.store().mock());
        this.workspaceGraphService = new WorkspaceGraphServiceImpl(
                flakeDefinitionService,
                lockFileService,
                nixCommandService
        );
        this.driftCompareService = new DriftCompareServiceImpl();
        this.driftReportMapper = new DriftReportMapper();
        this.driftCheckService = new DriftCheckServiceImpl(
                workspaceGraphService,
                liveStateService,
                driftCompareService,
                driftReportMapper);
        this.updateService = new UpdateServiceImpl(
                workspaceGraphService,
                driftCompareService,
                nixCommandService);
    }

    public AppConfiguration configuration() {
        return configuration;
    }

    public IDriftCheckService driftCheck() {
        return driftCheckService;
    }

    public IUpdateService update() {
        return updateService;
    }
}
