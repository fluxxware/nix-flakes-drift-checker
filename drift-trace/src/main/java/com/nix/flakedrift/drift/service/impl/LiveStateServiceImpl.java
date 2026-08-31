package com.nix.flakedrift.drift.service.impl;
import com.nix.flakedrift.drift.service.ILiveStateService;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nix.flakedrift.drift.domain.model.FlakeDependencyGraph;
import com.nix.flakedrift.drift.domain.model.FlakeGraphNode;
import com.nix.flakedrift.drift.infra.INixCommandService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * {@link ILiveStateService} — probes the target store for realized source objects
 * ({@code -source}/{@code -<name>}) and returns per-node presence matched by narHash.
 */
public final class LiveStateServiceImpl implements ILiveStateService {
    private static final Path DEFAULT_STORE = Path.of("/nix/store");

    private final INixCommandService nixCommandService;
    private final Path storeRoot;
    private final boolean mockStore;
    private final ObjectMapper mapper = new ObjectMapper();

    public LiveStateServiceImpl(INixCommandService nixCommandService) {
        this(nixCommandService, DEFAULT_STORE, false);
    }

    /** Points the probe at an explicit store root (real {@code /nix/store} or a mock store dir). */
    public LiveStateServiceImpl(INixCommandService nixCommandService, Path storeRoot) {
        this(nixCommandService, storeRoot, false);
    }

    /**
     * Points the probe at an explicit store root.
     *
     * @param mockStore when {@code true}, store objects are plain directories hashed
     *                  with {@code nix hash path} (simulated deployment); when {@code false}
     *                  the store is the real nix store probed via {@code nix path-info}.
     */
    public LiveStateServiceImpl(INixCommandService nixCommandService, Path storeRoot, boolean mockStore) {
        this.nixCommandService = nixCommandService;
        this.storeRoot = storeRoot;
        this.mockStore = mockStore;
    }

    @Override
    public Map<FlakeGraphNode, Boolean> probe(FlakeDependencyGraph graph) {
        Set<String> realizedHashes = scanStoreSources(sourceNames(graph));
        Map<FlakeGraphNode, Boolean> result = new HashMap<>();
        for (FlakeGraphNode node : graph.allNodes()) {
            if (node.getPath() == null) {
                continue;
            }
            // The scanned machine root is inherently present: it is the deployment being checked.
            if (node.equals(graph.getRoot())) {
                result.put(node, true);
                continue;
            }
            String targetHash = node.getLockHash() != null ? node.getLockHash() : node.getDiskHash();
            if (targetHash != null) {
                result.put(node, realizedHashes.contains(targetHash));
            }
        }
        return result;
    }

    private static List<String> sourceNames(FlakeDependencyGraph graph) {
        List<String> names = new ArrayList<>();
        for (FlakeGraphNode node : graph.allNodes()) {
            if (node.getPath() != null) {
                names.add(node.getPath().getFileName().toString());
            }
        }
        return names;
    }

    private Set<String> scanStoreSources(List<String> sourceNames) {
        List<Path> candidates = new ArrayList<>();
        for (Path p : listStorePaths()) {
            String name = p.getFileName().toString();
            if (name.endsWith("-source")) {
                candidates.add(p);
            } else {
                for (String n : sourceNames) {
                    if (name.endsWith("-" + n)) {
                        candidates.add(p);
                        break;
                    }
                }
            }
        }
        return collectNarHashes(candidates);
    }

    private List<Path> listStorePaths() {
        try (var stream = Files.list(storeRoot)) {
            return stream.toList();
        } catch (IOException e) {
            throw new IllegalStateException("cannot list " + storeRoot, e);
        }
    }

    private Set<String> collectNarHashes(List<Path> paths) {
        Set<String> out = new HashSet<>();
        if (paths.isEmpty()) {
            return out;
        }
        if (!mockStore) {
            // Real /nix/store: batch via nix path-info.
            List<String> asStrings = paths.stream().map(Path::toString).toList();
            try {
                out.addAll(parseNarHashes(nixCommandService.pathInfoJsonMany(asStrings)));
            } catch (Exception ignored) {
                // store may be unavailable — treat as no sources realized.
            }
            return out;
        }
        // Mock store: a plain directory tree that mirrors the real nix store shape
        // /nix/store/<hash>-source/<flake-name>/. The realized content that carries
        // the lock narHash is the inner <flake-name>/ directory, so hash that; fall
        // back to hashing the entry itself if it has no inner subdirs.
        for (Path candidate : paths) {
            try {
                List<Path> inner = new ArrayList<>();
                try (var stream = Files.list(candidate)) {
                    stream.filter(Files::isDirectory).forEach(inner::add);
                }
                if (inner.isEmpty()) {
                    out.add(nixCommandService.hashPath(candidate));
                } else {
                    for (Path sub : inner) {
                        out.add(nixCommandService.hashPath(sub));
                    }
                }
            } catch (Exception ignored) {
                // object not hashable — skip.
            }
        }
        return out;
    }

    private Set<String> parseNarHashes(String json) {
        Set<String> out = new HashSet<>();
        try {
            JsonNode root = mapper.readTree(json);
            for (Iterator<JsonNode> it = root.elements(); it.hasNext(); ) {
                JsonNode narHash = it.next().path("narHash");
                if (narHash.isTextual()) {
                    out.add(narHash.asText());
                }
            }
        } catch (Exception ignored) {
            // unparseable JSON — ignore.
        }
        return out;
    }
}
