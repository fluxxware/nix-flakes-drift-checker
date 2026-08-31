package com.nix.flakedrift.drift.service.impl;

import com.nix.flakedrift.drift.domain.model.FlakeGraphNode;
import com.nix.flakedrift.drift.infra.INixCommandService;
import com.nix.flakedrift.drift.service.ILiveStateService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static com.nix.flakedrift.drift.service.impl.LiveStateServiceTestData.FALLBACK_DISK_HASH;
import static com.nix.flakedrift.drift.service.impl.LiveStateServiceTestData.REALIZED_HASH;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Builds a live-state probe for a {@link LiveStateServiceTestData.TreeScenario}. The
 * scenario is a required dependency (constructor); the temp store root is injected with
 * {@link #withStoreRoot(Path)}; the probe runs in {@link #build()}.
 */
public final class LiveStateServiceTestBuilder {
    private final LiveStateServiceTestData.TreeScenario scenario;
    private Path storeRoot;

    public LiveStateServiceTestBuilder(LiveStateServiceTestData.TreeScenario scenario) {
        this.scenario = scenario;
    }

    public LiveStateServiceTestBuilder withStoreRoot(Path storeRoot) {
        this.storeRoot = storeRoot;
        return this;
    }

    public Map<FlakeGraphNode, Boolean> build() throws IOException {
        writeStoreObjects(storeRoot);

        INixCommandService nix = mock(INixCommandService.class);
        when(nix.pathInfoJsonMany(anyList())).thenAnswer(inv -> narHashJson(inv.getArgument(0)));

        ILiveStateService service = new LiveStateServiceImpl(nix, storeRoot);
        return service.probe(scenario.graph());
    }

    private static void writeStoreObjects(Path storeRoot) throws IOException {
        Files.writeString(storeRoot.resolve("abc-source"), "");
        Files.writeString(storeRoot.resolve("zzz-app"), "");
        Files.writeString(storeRoot.resolve("xyz-n3"), "");
    }

    private String narHashJson(List<String> requestedPaths) {
        StringBuilder json = new StringBuilder("{");
        for (int i = 0; i < requestedPaths.size(); i++) {
            if (i > 0) {
                json.append(',');
            }
            String storePath = requestedPaths.get(i);
            String narHash = switch (storePath) {
                case String s when s.equals(storeRoot.resolve("abc-source").toString()) -> REALIZED_HASH;
                case String s when s.equals(storeRoot.resolve("zzz-app").toString()) -> "sha256-UNUSED";
                case String s when s.equals(storeRoot.resolve("xyz-n3").toString()) -> FALLBACK_DISK_HASH;
                default -> "sha256-NONE";
            };
            json.append('"').append(storePath).append("\":{\"narHash\":\"").append(narHash).append("\"}");
        }
        json.append('}');
        return json.toString();
    }
}
