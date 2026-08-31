package com.nix.flakedrift.drift.dto;

/**
 * Outcome of updating a single flake's {@code flake.lock} — the fingerprint
 * (sha256 of the lock file content) before and after {@code nix flake update},
 * the raw nix output (revA → revB detail), and whether the lock changed at all.
 */
public record UpdateResultDto(
        String name,
        int depth,
        boolean changed,
        String lockBefore,
        String lockAfter,
        String nixOutput) {
}