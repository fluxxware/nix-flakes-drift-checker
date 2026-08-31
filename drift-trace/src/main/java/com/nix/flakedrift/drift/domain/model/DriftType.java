package com.nix.flakedrift.drift.domain.model;

/**
 * Classification of a single flake node's sync state across the
 * three hashes: disk (current content), lock (parent flake.lock),
 * live (present in target's nix store).
 */
public enum DriftType {
    /** disk == lock == live — fully in sync. */
    SYNC,

    /** disk != lock — local source edited, but the parent flake.lock was not re-locked. */
    LOCAL_DRIFT,

    /** lock entry exists but carries no narHash — disk vs lock cannot be compared. */
    NARHASH_ABSENT,

    /** lock != live — the locked version was never realized on the target. */
    UNDEPLOYED,

    /** the node is the origin of chain staleness — it is itself locally drifted. */
    CHAIN_STALE_CAUSE,

    /** the node is stale only because a locked descendant is a chain cause — it needs re-lock. */
    CHAIN_STALE_TRANSITIVE,

    /** remote (github/git) input — tracked by the lock only, not walked locally. */
    REMOTE
}
