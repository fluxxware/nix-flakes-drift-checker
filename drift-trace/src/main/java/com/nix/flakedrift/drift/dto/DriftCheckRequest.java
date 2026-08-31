package com.nix.flakedrift.drift.dto;

import com.nix.flakedrift.drift.domain.model.DeploymentTarget;

import java.nio.file.Path;

//TODO: Don't forget about deployment target once it's needed to remote-connect
public record DriftCheckRequest(Path flakeRootAbsolutePath, DeploymentTarget deploymentTarget) {
    public DriftCheckRequest(Path flakeRootAbsolutePath) {
        this(flakeRootAbsolutePath, DeploymentTarget.local());
    }
}
