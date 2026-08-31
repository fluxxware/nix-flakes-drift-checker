package com.nix.flakedrift.drift.domain.model;

/**
 * Deployment target being checked. Local uses the native nix store;
 * REMOTE (eta: once in whatever) will go through SSH.
 */
public record DeploymentTarget(TargetKind kind, String host) {
    public enum TargetKind { LOCAL, REMOTE }

    public static DeploymentTarget local() {
        return new DeploymentTarget(TargetKind.LOCAL, null);
    }

    public static DeploymentTarget remote(String host) {
        return new DeploymentTarget(TargetKind.REMOTE, host);
    }
}
