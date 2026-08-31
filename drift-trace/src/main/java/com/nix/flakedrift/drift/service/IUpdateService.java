package com.nix.flakedrift.drift.service;

import com.nix.flakedrift.drift.dto.UpdateCandidateDto;
import com.nix.flakedrift.drift.dto.UpdateResultDto;

import java.nio.file.Path;
import java.util.List;

/**
 * Computes the ordered chain of local flakes to {@code nix flake update} and
 * runs the update over it. Nix cannot update a whole tree from one command, so
 * each flake is updated individually, deepest-first, so a parent re-locks its
 * children's fresh narHashes.
 */
public interface IUpdateService {
    /** Local flakes needing update, ordered deepest level first (root last). */
    List<UpdateCandidateDto> findUpdateCandidates(Path flakeRootAbsolutePath);

    /**
     * Runs {@code nix flake update --flake <dir>} on every candidate, deepest first.
     * Returns the per-flake before/after lock fingerprints + raw nix output
     * (for the audit history). Aborts on first failure.
     */
    List<UpdateResultDto> updateAll(Path flakeRootAbsolutePath);
}