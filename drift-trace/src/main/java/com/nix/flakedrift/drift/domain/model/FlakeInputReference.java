package com.nix.flakedrift.drift.domain.model;

/** A single input reference declared in a flake.nix. */
public record FlakeInputReference(String name, String url) {
    public boolean isPath() {
        return url != null && url.startsWith("path:");
    }

    public String pathValue() {
        return url.substring("path:".length());
    }
}
