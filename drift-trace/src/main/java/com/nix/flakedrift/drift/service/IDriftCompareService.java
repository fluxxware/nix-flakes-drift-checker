package com.nix.flakedrift.drift.service;

import com.nix.flakedrift.drift.domain.model.DriftType;
import com.nix.flakedrift.drift.domain.model.FlakeDependencyGraph;
import com.nix.flakedrift.drift.domain.model.FlakeGraphNode;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Pure per-status predicates over a node's hashes. No mutation — the full
 * {@link Set} of a node's statuses is derived, and {@link #evaluate} returns
 * the whole tree's statuses as a value map.
 */
public interface IDriftCompareService {
    /** {@code lockHash == null} — the parent lock entry carries no narHash. */
    boolean classifyNarHashAbsent(FlakeGraphNode node);

    /** {@code lock != null && disk != lock} — source edited, parent lock not re-locked. */
    boolean classifyLocalDrift(FlakeGraphNode node);

    /** {@code lock != null && disk == lock && !live} — locked version never realized. */
    boolean classifyUndeployed(FlakeGraphNode node, boolean livePresent);

    /** {@code path == null} — remote (github/git) input. */
    boolean classifyRemote(FlakeGraphNode node);

    /** the node is locally drifted — it is the origin of any chain staleness. */
    boolean classifyChainCause(FlakeGraphNode node);

    /**
     * the node is not drifted itself, but has a descendant that is a chain cause
     * (or transitive) — its own lock must be refreshed to pick the change up.
     */
    boolean classifyChainTransitive(FlakeGraphNode node, List<Set<DriftType>> childTypes);

    /**
     * Composes all predicates; returns the node's full status set.
     * An empty set means fully in sync; {@code {REMOTE}} for remote inputs.
     */
    Set<DriftType> classify(FlakeGraphNode node, List<Set<DriftType>> childTypes, boolean livePresent);

    /**
     * Walks the tree children-first and returns each node's full status set.
     * {@code live} is the per-node presence map from the live probe.
     */
    Map<FlakeGraphNode, Set<DriftType>> evaluate(FlakeDependencyGraph tree, Map<FlakeGraphNode, Boolean> live);
}