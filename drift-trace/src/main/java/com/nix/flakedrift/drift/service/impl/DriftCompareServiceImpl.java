package com.nix.flakedrift.drift.service.impl;
import com.nix.flakedrift.drift.service.IDriftCompareService;

import com.nix.flakedrift.drift.domain.model.DriftType;
import com.nix.flakedrift.drift.domain.model.FlakeDependencyGraph;
import com.nix.flakedrift.drift.domain.model.FlakeGraphNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** {@link IDriftCompareService} — pure per-status predicates + composition. */
public final class DriftCompareServiceImpl implements IDriftCompareService {
    @Override
    public boolean classifyNarHashAbsent(FlakeGraphNode node) {
        return node.getLockHash() == null;
    }

    @Override
    public boolean classifyLocalDrift(FlakeGraphNode node) {
        return !classifyNarHashAbsent(node)
                && !Objects.equals(node.getDiskHash(), node.getLockHash());
    }

    @Override
    public boolean classifyUndeployed(FlakeGraphNode node, boolean livePresent) {
        return !classifyNarHashAbsent(node)
                && Objects.equals(node.getDiskHash(), node.getLockHash())
                && !livePresent;
    }

    @Override
    public boolean classifyRemote(FlakeGraphNode node) {
        return node.getPath() == null;
    }

    @Override
    public boolean classifyChainCause(FlakeGraphNode node) {
        return classifyLocalDrift(node);
    }

    @Override
    public boolean classifyChainTransitive(FlakeGraphNode node, List<Set<DriftType>> childTypes) {
        if (childTypes == null) {
            return false;
        }
        return childTypes.stream().anyMatch(t -> t.contains(DriftType.CHAIN_STALE_CAUSE)
                || t.contains(DriftType.CHAIN_STALE_TRANSITIVE));
    }

    @Override
    public Set<DriftType> classify(FlakeGraphNode node, List<Set<DriftType>> childTypes, boolean livePresent) {
        if (classifyRemote(node)) {
            return Set.of(DriftType.REMOTE);
        }
        Set<DriftType> types = new LinkedHashSet<>();
        if (classifyNarHashAbsent(node)) {
            types.add(DriftType.NARHASH_ABSENT);
        }
        if (classifyLocalDrift(node)) {
            types.add(DriftType.LOCAL_DRIFT);
        }
        if (classifyUndeployed(node, livePresent)) {
            types.add(DriftType.UNDEPLOYED);
        }
        if (classifyChainCause(node)) {
            types.add(DriftType.CHAIN_STALE_CAUSE);
        }
        if (classifyChainTransitive(node, childTypes)) {
            types.add(DriftType.CHAIN_STALE_TRANSITIVE);
        }
        return types;
    }

    @Override
    public Map<FlakeGraphNode, Set<DriftType>> evaluate(FlakeDependencyGraph tree,
                                                        Map<FlakeGraphNode, Boolean> live) {
        Map<FlakeGraphNode, Set<DriftType>> result = new HashMap<>();
        for (FlakeGraphNode node : postOrder(tree.getRoot())) {
            List<Set<DriftType>> childTypes = node.getChildren().stream()
                    .map(result::get)
                    .toList();
            boolean livePresent = live.getOrDefault(node, false);
            result.put(node, classify(node, childTypes, livePresent));
        }
        return result;
    }

    private static List<FlakeGraphNode> postOrder(FlakeGraphNode node) {
        List<FlakeGraphNode> out = new ArrayList<>();
        for (FlakeGraphNode child : node.getChildren()) {
            out.addAll(postOrder(child));
        }
        out.add(node);
        return out;
    }
}