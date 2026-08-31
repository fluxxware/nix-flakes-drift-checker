package com.nix.flakedrift.drift.infra;

import com.nix.flakedrift.drift.domain.model.FlakeInputReference;
import com.nix.flakedrift.drift.infra.impl.FlakeDefinitionService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Builds a parse of {@link FlakeDefinitionService} with the given flake.nix.
 * The working directory is a required dependency (constructor); the flake.nix body is
 * optional and set with {@link #withFlakeNix(String)}; the parse runs in {@link #build()}.
 */
public final class FlakeDefinitionServiceTestBuilder {
    private final Path directory;
    private String flakeNixBody = "";

    public FlakeDefinitionServiceTestBuilder(Path directory) {
        this.directory = directory;
    }

    public FlakeDefinitionServiceTestBuilder withFlakeNix(String body) {
        this.flakeNixBody = body;
        return this;
    }

    public List<FlakeInputReference> build() throws IOException {
        if (!flakeNixBody.isEmpty()) {
            Files.writeString(directory.resolve("flake.nix"), flakeNixBody);
        }
        return new FlakeDefinitionService().parseInputs(directory.resolve("flake.nix"));
    }
}
