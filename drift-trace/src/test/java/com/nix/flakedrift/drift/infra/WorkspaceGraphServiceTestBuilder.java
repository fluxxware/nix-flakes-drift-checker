package com.nix.flakedrift.drift.infra;

import com.nix.flakedrift.drift.infra.impl.FlakeDefinitionService;
import com.nix.flakedrift.drift.infra.impl.LockFileService;
import com.nix.flakedrift.drift.service.IWorkspaceGraphService;
import com.nix.flakedrift.drift.service.impl.WorkspaceGraphServiceImpl;

import java.nio.file.Path;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Builds a {@link IWorkspaceGraphService} over a flake fixture tree. Nix hashing is
 * mocked deterministically (no /nix/store needed): any path hashes to a value derived
 * from the path, so per-node disk hashes are stable across runs.
 */
public final class WorkspaceGraphServiceTestBuilder {
    private final INixCommandService nix = mock(INixCommandService.class);

    public WorkspaceGraphServiceTestBuilder withSetupNixCommandMock() {
        when(nix.hashPath(any(Path.class))).thenAnswer(inv -> "sha256-" + inv.getArgument(0));
        return this;
    }

    public IWorkspaceGraphService build() {
        return new WorkspaceGraphServiceImpl(
                new FlakeDefinitionService(),
                new LockFileService(),
                nix);
    }
}
