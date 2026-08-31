package com.nix.flakedrift.drift.infra.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nix.flakedrift.drift.domain.model.FlakeLockEntry;
import com.nix.flakedrift.drift.infra.ILockFileService;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public final class LockFileService implements ILockFileService {
    private static final String DEPENDENCY_TREES_CONTAINER = "nodes";
    private static final String IMMUTABLE_METADATA_SNAPSHOT = "locked";
    private static final String ARCHIVE_INTEGRITY_HASH = "narHash";
    private static final String LOCAL_SOURCE_PATH = "path";
    private static final String SOURCE_FETCH_METHOD = "type";

    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * Parses funny JSON entry like this
     * {@snippet lang="json" :
     * {
     *   "nodes": {
     *     "agentic": {
     *       "inputs": {
     *         "nixpkgs": "nixpkgs_7"
     *       },
     *       "locked": {
     *         "lastModified": 1786705083,
     *         "narHash": "sha256-Kt3+iZEzLAQaMVzW+hcNmZX9VlaBqxYaDLzb7V6+SkU=",
     *         "path": "/etc/nixos/your-funny-fleet-of-flakes/serious-flake-1",
     *         "type": "path"
     *       },
     *       "original": {
     *         "path": "/etc/nixos/your-funny-fleet-of-flakes/serious-flake-1",
     *         "type": "path"
     *       }
     *     }
     *   }
     * }
     * }
     * Into map of FlakeLockEntries
     */
    @Override
    public Map<String, FlakeLockEntry> readLockFile(Path flakeLockPath) {
        Map<String, FlakeLockEntry> output = new HashMap<>();
        if (flakeLockPath == null || !Files.isRegularFile(flakeLockPath)) {
            return output;
        }
        try {
            JsonNode root = mapper.readTree(flakeLockPath.toFile());
            JsonNode nodes = root.path(DEPENDENCY_TREES_CONTAINER);
            for (Iterator<Map.Entry<String, JsonNode>> it = nodes.fields(); it.hasNext(); ) {
                Map.Entry<String, JsonNode> jsonNodeEntry = it.next();
                JsonNode currentlyUsedSnapshot = jsonNodeEntry.getValue().path(IMMUTABLE_METADATA_SNAPSHOT);
                output.put(jsonNodeEntry.getKey(), new FlakeLockEntry(
                        jsonNodeEntry.getKey(),
                        currentlyUsedSnapshot.path(ARCHIVE_INTEGRITY_HASH).asText(null),
                        currentlyUsedSnapshot.path(LOCAL_SOURCE_PATH).asText(null),
                        currentlyUsedSnapshot.path(SOURCE_FETCH_METHOD).asText(null)
                ));
            }
        } catch (Exception ex) {
            throw new IllegalStateException("cannot parse flake.lock: " + flakeLockPath, ex);
        }
        return output;
    }
}
