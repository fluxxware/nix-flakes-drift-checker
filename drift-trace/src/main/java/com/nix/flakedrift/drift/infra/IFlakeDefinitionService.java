package com.nix.flakedrift.drift.infra;

import com.nix.flakedrift.drift.domain.model.FlakeInputReference;

import java.nio.file.Path;
import java.util.List;

/** Extracts declared inputs from a flake.nix. */
public interface IFlakeDefinitionService {
    /** Declared inputs with their url (path: / github: ...). */
    List<FlakeInputReference> parseInputs(Path flakeNixPath);
}
