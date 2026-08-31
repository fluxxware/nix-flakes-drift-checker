package com.nix.flakedrift.drift.dto;

import com.nix.flakedrift.drift.domain.model.DriftType;

import java.util.Set;

/**
 * A direct child whose lock entry needs refreshing in the parent — the child is
 * a {@code CHAIN_STALE_CAUSE} or {@code CHAIN_STALE_TRANSITIVE} and its link is
 * locked (carries a narHash), so the parent must re-lock it.
 */
public record DriftMemberDto(String name, Set<DriftType> types) {
}
