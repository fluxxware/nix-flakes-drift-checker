package com.nix.flakedrift.drift.dto;

import java.nio.file.Path;

/**
 * A local flake that needs {@code nix flake update}, ordered deepest-first.
 * Remote (non-path) inputs are never candidates — they are refreshed when
 * their parent flake is updated.
 */
public record UpdateCandidateDto(String name, Path path, int depth) {
}