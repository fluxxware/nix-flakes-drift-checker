package com.nix.flakedrift.drift.service.impl;

import com.nix.flakedrift.drift.domain.model.FlakeDependencyGraph;
import com.nix.flakedrift.drift.dto.UpdateCandidateDto;
import com.nix.flakedrift.drift.infra.INixCommandService;
import com.nix.flakedrift.drift.service.IUpdateService;
import com.nix.flakedrift.drift.service.IWorkspaceGraphService;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Builds a wired {@link IUpdateService}. The scenario tree (via
 * {@link UpdateServiceImplTestData.Scenario}) or a real-disk tree (via
 * {@link UpdateServiceImplTestData.LocalFlakeTree}) is a required dependency
 * (constructor); the workspace-graph mock is wired via
 * {@link #withSetupWorkspaceGraphMock()}, and {@link #build()} exposes the service.
 * The nix mock stays reachable via {@link #nix()} so tests can assert command
 * order or stub real lock rewrites.
 */
public final class UpdateServiceImplTestBuilder {
    private final IWorkspaceGraphService graphService = mock(IWorkspaceGraphService.class);
    private final INixCommandService nix = mock(INixCommandService.class);
    private final FlakeDependencyGraph graph;
    private final java.util.List<UpdateCandidateDto> chain;

    public UpdateServiceImplTestBuilder(UpdateServiceImplTestData.Scenario scenario) {
        this.graph = scenario.tree();
        this.chain = java.util.List.of();
    }

    public UpdateServiceImplTestBuilder(UpdateServiceImplTestData.LocalFlakeTree tree) {
        this.graph = tree.graph();
        this.chain = tree.updateChain();
    }

    public UpdateServiceImplTestBuilder withSetupWorkspaceGraphMock() {
        when(graphService.buildDependencyGraph(any(Path.class))).thenReturn(graph);
        return this;
    }

    /** Writes an initial {@code flake.lock} into every candidate dir (a real-disk tree fixture only). */
    public UpdateServiceImplTestBuilder withMaterializedLocks() {
        try {
            for (UpdateCandidateDto candidate : chain) {
                Path lock = candidate.path().resolve("flake.lock");
                Files.createDirectories(lock.getParent());
                Files.writeString(lock, "old-lock-" + candidate.name());
            }
            return this;
        } catch (java.io.IOException e) {
            throw new IllegalStateException("cannot materialize lock files", e);
        }
    }

    /** The recorded nix mock — tests verify {@code run(...)} order against it. */
    public INixCommandService nix() {
        return nix;
    }

    public IUpdateService build() {
        return new UpdateServiceImpl(graphService, new DriftCompareServiceImpl(), nix);
    }
}