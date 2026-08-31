package com.nix.flakedrift.drift.service;

import com.nix.flakedrift.drift.domain.model.FlakeDependencyGraph;
import com.nix.flakedrift.drift.domain.model.FlakeGraphNode;

import java.util.Map;

/** Probes the live target store and returns per-node presence as a value (no mutation). */
public interface ILiveStateService {
    /**
     * Scans the target store for realized source objects and returns, for every
     * non-remote node, whether its reference hash ({@code lock ?? disk}) is present.
     */
    Map<FlakeGraphNode, Boolean> probe(FlakeDependencyGraph graph);
}