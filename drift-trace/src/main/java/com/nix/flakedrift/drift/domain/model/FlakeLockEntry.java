package com.nix.flakedrift.drift.domain.model;

/** A locked input entry from a flake.lock node. */
public record FlakeLockEntry(String name, String narHash, String path, String type) {
    public boolean isPath() {
        return "path".equals(type) || path != null;
    }
}
