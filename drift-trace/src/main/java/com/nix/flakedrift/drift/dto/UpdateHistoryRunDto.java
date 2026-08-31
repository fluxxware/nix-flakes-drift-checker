package com.nix.flakedrift.drift.dto;

import java.util.List;

/**
 * One {@code update} run — the full audit record: when/for which root the run
 * happened, which flakes were updated in which order, per-flake before/after
 * lock fingerprints (+ raw nix output so "input X: revA → revB" is never lost),
 * and a summary of changed/unchanged. JSON shape (pretty-printed):
 *
 * <pre>{@code
 * { "timestamp": "2026-08-25T15:33:00Z", "toolVersion": "1.0-SNAPSHOT",
 *   "root": "/etc/nixos",
 *   "flakes": [ { "name": "flake-03", "depth": 3, "changed": true, ... } ],
 *   "summary": { "total": 5, "changed": 5, "unchanged": 0 } }
 * }</pre>
 */
public record UpdateHistoryRunDto(
        String timestamp,
        String toolVersion,
        String root,
        List<UpdateResultDto> flakes,
        Summary summary) {

    /** {@code total / changed / unchanged} counts for the run. */
    public record Summary(int total, int changed, int unchanged) {
    }

    public static UpdateHistoryRunDto of(String timestamp, String toolVersion, String root, List<UpdateResultDto> flakes) {
        int changed = (int) flakes.stream().filter(UpdateResultDto::changed).count();
        return new UpdateHistoryRunDto(
                timestamp, toolVersion, root, flakes,
                new Summary(flakes.size(), changed, flakes.size() - changed));
    }
}