package com.nix.flakedrift.drift.infra;

import java.nio.file.Path;
import java.util.List;

/** Runs nix commands via a subprocess. */
public interface INixCommandService {
    /** {@code nix hash path <dir>} → SRI narHash of the directory content. */
    String hashPath(Path dir);

    /** {@code nix path-info --json} over many store paths in one call. */
    String pathInfoJsonMany(List<String> storePaths);

    /** Raw subprocess runner for custom nix invocations. */
    String run(List<String> args);
}
