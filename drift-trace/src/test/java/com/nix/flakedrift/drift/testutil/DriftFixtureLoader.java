package com.nix.flakedrift.drift.testutil;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nix.flakedrift.drift.domain.model.DriftType;
import com.nix.flakedrift.drift.domain.model.FlakeDependencyGraph;
import com.nix.flakedrift.drift.domain.model.FlakeGraphNode;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Loads a flat JSON dataset into a {@link FlakeDependencyGraph} plus the live map
 * and the expected statuses/unrefreshed/totals, so tests can assert drift evaluation.
 *
 * <p>Nodes are a flat pre-order array; children are referenced by name. When a name
 * repeats, each parent takes the next unused instance that appears after it.
 */
public final class DriftFixtureLoader {
    private final ObjectMapper mapper = new ObjectMapper();

    public record Totals(int total, int synced, int drifted) {
    }

    public record Loaded(FlakeDependencyGraph graph,
                         Map<FlakeGraphNode, Boolean> live,
                         Map<String, Set<DriftType>> expectedStatuses,
                         Map<String, List<String>> expectedUnrefreshed,
                         Totals expectedTotals) {
    }

    public Loaded load(Path jsonPath) throws IOException {
        return load(mapper.readTree(jsonPath.toFile()));
    }

    private Loaded load(JsonNode root) throws IOException {
        JsonNode nodes = root.path("nodes");
        List<FlakeGraphNode> instances = new ArrayList<>();
        Map<String, List<Integer>> byName = new HashMap<>();

        for (int i = 0; i < nodes.size(); i++) {
            JsonNode n = nodes.get(i);
            String name = n.path("name").asText();
            String path = n.path("path").isNull() ? null : n.path("path").asText();
            FlakeGraphNode node = new FlakeGraphNode(name, path == null ? null : Path.of(path));
            node.setDiskHash(n.path("diskHash").isNull() ? null : n.path("diskHash").asText());
            node.setLockHash(n.path("lockHash").isNull() ? null : n.path("lockHash").asText());
            instances.add(node);
            byName.computeIfAbsent(name, k -> new ArrayList<>()).add(i);
        }

        boolean[] consumed = new boolean[instances.size()];
        for (int i = 0; i < nodes.size(); i++) {
            FlakeGraphNode parent = instances.get(i);
            for (JsonNode childName : nodes.get(i).path("children")) {
                int childIndex = nextUnused(byName, consumed, childName.asText(), i);
                consumed[childIndex] = true;
                parent.addChild(instances.get(childIndex));
            }
        }

        Map<FlakeGraphNode, Boolean> live = new HashMap<>();
        JsonNode liveNode = root.path("live");
        for (FlakeGraphNode node : instances) {
            JsonNode value = liveNode.get(node.getName());
            if (value != null && value.isBoolean()) {
                live.put(node, value.asBoolean());
            }
        }

        Map<String, Set<DriftType>> statuses = new HashMap<>();
        JsonNode statusNode = root.path("expected").path("statuses");
        for (FlakeGraphNode node : instances) {
            Set<DriftType> types = new LinkedHashSet<>();
            for (JsonNode s : statusNode.path(node.getName())) {
                types.add(DriftType.valueOf(s.asText()));
            }
            statuses.put(node.getName(), types);
        }

        Map<String, List<String>> unrefreshed = new HashMap<>();
        JsonNode unrefreshedNode = root.path("expected").path("unrefreshed");
        for (FlakeGraphNode node : instances) {
            List<String> names = new ArrayList<>();
            for (JsonNode m : unrefreshedNode.path(node.getName())) {
                names.add(m.asText());
            }
            unrefreshed.put(node.getName(), names);
        }

        JsonNode totals = root.path("expected").path("totals");
        Totals expectedTotals = new Totals(
                totals.path("total").asInt(),
                totals.path("synced").asInt(),
                totals.path("drifted").asInt());

        return new Loaded(new FlakeDependencyGraph(instances.get(0)), live, statuses, unrefreshed, expectedTotals);
    }

    private static int nextUnused(Map<String, List<Integer>> byName, boolean[] consumed,
                                  String name, int parentIndex) {
        for (int i : byName.getOrDefault(name, List.of())) {
            if (i > parentIndex && !consumed[i]) {
                return i;
            }
        }
        throw new IllegalStateException("no unused node '" + name + "' after index " + parentIndex);
    }
}
