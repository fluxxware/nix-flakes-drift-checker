package com.nix.flakedrift.drift.dto;

import com.nix.flakedrift.drift.domain.model.DriftType;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Flat, JSON-friendly view of a flake node for output.
 *
 * <p>{@link #driftTypes} carries the full set of independent statuses — a node
 * can be simultaneously {@code NARHASH_ABSENT} and a {@code CHAIN_STALE_TRANSITIVE}
 * member of the re-lock chain. An empty set means fully in sync.
 */
public class FlakeGraphNodeDto {
    public String name;
    public String path;
    public String diskHash;
    public String lockHash;
    public boolean livePresent;
    public Set<DriftType> driftTypes = new HashSet<>();
    public List<DriftMemberDto> unrefreshedMembers = new ArrayList<>();
    public List<FlakeGraphNodeDto> children = new ArrayList<>();
}
