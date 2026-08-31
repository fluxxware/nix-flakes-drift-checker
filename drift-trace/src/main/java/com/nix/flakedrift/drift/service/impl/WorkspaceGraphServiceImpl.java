package com.nix.flakedrift.drift.service.impl;
import com.nix.flakedrift.drift.service.IWorkspaceGraphService;

import com.nix.flakedrift.drift.domain.model.FlakeDependencyGraph;
import com.nix.flakedrift.drift.domain.model.FlakeGraphNode;
import com.nix.flakedrift.drift.domain.model.FlakeInputReference;
import com.nix.flakedrift.drift.domain.model.FlakeLockEntry;
import com.nix.flakedrift.drift.infra.IFlakeDefinitionService;
import com.nix.flakedrift.drift.infra.ILockFileService;
import com.nix.flakedrift.drift.infra.INixCommandService;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/** {@link IWorkspaceGraphService} — recursive walk of {@code path:} inputs. */
public final class WorkspaceGraphServiceImpl implements IWorkspaceGraphService {
    private final IFlakeDefinitionService flakeDefinitionService;
    private final ILockFileService lockFileService;
    private final INixCommandService nixCommandService;

    public WorkspaceGraphServiceImpl(
            IFlakeDefinitionService flakeDefinitionService,
            ILockFileService lockFileService,
            INixCommandService nixCommandService) {
        this.flakeDefinitionService = flakeDefinitionService;
        this.lockFileService = lockFileService;
        this.nixCommandService = nixCommandService;
    }

    @Override
    public FlakeDependencyGraph buildDependencyGraph(Path rootDir) {
        FlakeGraphNode flakeGraphNode = new FlakeGraphNode("root", rootDir);
        flakeGraphNode.setDiskHash(nixCommandService.hashPath(rootDir));
        // The root has no parent lock; treat its disk hash as its own reference.
        flakeGraphNode.setLockHash(flakeGraphNode.getDiskHash());
        walk(flakeGraphNode);
        return new FlakeDependencyGraph(flakeGraphNode);
    }

    private void walk(FlakeGraphNode flakeGraphNode) {
        Path flakeNix = flakeGraphNode.getPath().resolve("flake.nix");
        if (!Files.isRegularFile(flakeNix)) {
            return;
        }
        Map<String, FlakeLockEntry> locked = lockFileService.readLockFile(flakeGraphNode.getPath().resolve("flake.lock"));
        for (FlakeInputReference ref : flakeDefinitionService.parseInputs(flakeNix)) {
            if (!ref.isPath()) {
                FlakeGraphNode remote = new FlakeGraphNode(ref.name(), null);
                remote.setDepth(flakeGraphNode.getDepth() + 1);
                flakeGraphNode.addChild(remote);
                continue;
            }
            Path childDir = resolve(flakeGraphNode.getPath(), ref.pathValue());
            FlakeGraphNode child = new FlakeGraphNode(ref.name(), childDir);
            child.setDepth(flakeGraphNode.getDepth() + 1);
            child.setDiskHash(nixCommandService.hashPath(childDir));
            FlakeLockEntry entry = locked.get(ref.name());
            if (entry != null && entry.narHash() != null) {
                child.setLockHash(entry.narHash());
            }
            flakeGraphNode.addChild(child);
            walk(child);
        }
    }

    private static Path resolve(Path parent, String value) {
        Path p = Path.of(value);
        return p.isAbsolute() ? p.normalize() : parent.resolve(p).normalize();
    }
}
