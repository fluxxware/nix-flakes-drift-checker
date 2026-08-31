package com.nix.flakedrift.drift.domain.model;

import java.util.ArrayList;
import java.util.List;

/**
 * The workspace dependency DAG: root flake with nested path: inputs.
 */
public class FlakeDependencyGraph {
    private final FlakeGraphNode root;

    public FlakeDependencyGraph(FlakeGraphNode root) {
        this.root = root;
    }

    public FlakeGraphNode getRoot() {
        return root;
    }

    /** Depth-first flattening of all nodes (root first). */
    public List<FlakeGraphNode> allNodes() {
        List<FlakeGraphNode> out = new ArrayList<>();
        collect(root, out);
        return out;
    }

    private void collect(FlakeGraphNode node, List<FlakeGraphNode> acc) {
        acc.add(node);
        for (FlakeGraphNode child : node.getChildren()) {
            collect(child, acc);
        }
    }
}
