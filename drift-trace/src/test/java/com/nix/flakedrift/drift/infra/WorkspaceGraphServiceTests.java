package com.nix.flakedrift.drift.infra;

import com.nix.flakedrift.drift.domain.model.FlakeGraphNode;
import com.nix.flakedrift.drift.service.IWorkspaceGraphService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static com.nix.flakedrift.drift.infra.WorkspaceGraphServiceTestData.AGGREGATOR;
import static com.nix.flakedrift.drift.infra.WorkspaceGraphServiceTestData.CLEAN_APP;
import static com.nix.flakedrift.drift.infra.WorkspaceGraphServiceTestData.CLEAN_APP_DIR;
import static com.nix.flakedrift.drift.infra.WorkspaceGraphServiceTestData.CLEAN_APP_HASH;
import static com.nix.flakedrift.drift.infra.WorkspaceGraphServiceTestData.CLEAN_FIXTURE;
import static com.nix.flakedrift.drift.infra.WorkspaceGraphServiceTestData.CLEAN_SERVICE;
import static com.nix.flakedrift.drift.infra.WorkspaceGraphServiceTestData.CLEAN_SERVICE_DIR;
import static com.nix.flakedrift.drift.infra.WorkspaceGraphServiceTestData.CLEAN_SERVICE_HASH;
import static com.nix.flakedrift.drift.infra.WorkspaceGraphServiceTestData.CLEAN_UPSTREAM_1;
import static com.nix.flakedrift.drift.infra.WorkspaceGraphServiceTestData.DEEP_CHAIN_FIXTURE;
import static com.nix.flakedrift.drift.infra.WorkspaceGraphServiceTestData.DEEP_LAYER_1;
import static com.nix.flakedrift.drift.infra.WorkspaceGraphServiceTestData.DEEP_LAYER_1_DIR;
import static com.nix.flakedrift.drift.infra.WorkspaceGraphServiceTestData.DEEP_LAYER_2;
import static com.nix.flakedrift.drift.infra.WorkspaceGraphServiceTestData.DEEP_LAYER_2_DIR;
import static com.nix.flakedrift.drift.infra.WorkspaceGraphServiceTestData.DEEP_LAYER_3;
import static com.nix.flakedrift.drift.infra.WorkspaceGraphServiceTestData.DEEP_LAYER_3_DIR;
import static com.nix.flakedrift.drift.infra.WorkspaceGraphServiceTestData.DEEP_LAYER1_HASH;
import static com.nix.flakedrift.drift.infra.WorkspaceGraphServiceTestData.DEEP_LAYER2_HASH;
import static com.nix.flakedrift.drift.infra.WorkspaceGraphServiceTestData.DEEP_LAYER3_HASH;
import static com.nix.flakedrift.drift.infra.WorkspaceGraphServiceTestData.FIXTURE_ROOT;
import static com.nix.flakedrift.drift.infra.WorkspaceGraphServiceTestData.HOME_MANAGER;
import static com.nix.flakedrift.drift.infra.WorkspaceGraphServiceTestData.HUB_FLAKE;
import static com.nix.flakedrift.drift.infra.WorkspaceGraphServiceTestData.HUB_FLAKE_HASH;
import static com.nix.flakedrift.drift.infra.WorkspaceGraphServiceTestData.LEAF_APP;
import static com.nix.flakedrift.drift.infra.WorkspaceGraphServiceTestData.LEAF_APP_HASH;
import static com.nix.flakedrift.drift.infra.WorkspaceGraphServiceTestData.LEAF_MODULE;
import static com.nix.flakedrift.drift.infra.WorkspaceGraphServiceTestData.LEAF_MODULE_HASH;
import static com.nix.flakedrift.drift.infra.WorkspaceGraphServiceTestData.MULTI_CAUSE_FIXTURE;
import static com.nix.flakedrift.drift.infra.WorkspaceGraphServiceTestData.MULTI_HUB;
import static com.nix.flakedrift.drift.infra.WorkspaceGraphServiceTestData.MULTI_HUB_HASH;
import static com.nix.flakedrift.drift.infra.WorkspaceGraphServiceTestData.MULTI_LAYER;
import static com.nix.flakedrift.drift.infra.WorkspaceGraphServiceTestData.MULTI_LAYER_HASH;
import static com.nix.flakedrift.drift.infra.WorkspaceGraphServiceTestData.MULTI_LEAF_A;
import static com.nix.flakedrift.drift.infra.WorkspaceGraphServiceTestData.MULTI_LEAF_A_HASH;
import static com.nix.flakedrift.drift.infra.WorkspaceGraphServiceTestData.REMOTE_INPUT;
import static com.nix.flakedrift.drift.infra.WorkspaceGraphServiceTestData.STAGING_AGGREGATOR;
import static com.nix.flakedrift.drift.infra.WorkspaceGraphServiceTestData.STAGING_AGGREGATOR_INPUTS;
import static com.nix.flakedrift.drift.infra.WorkspaceGraphServiceTestData.STAGING_FIXTURE;
import static com.nix.flakedrift.drift.infra.WorkspaceGraphServiceTestData.STAGING_ROOT;
import static com.nix.flakedrift.drift.infra.WorkspaceGraphServiceTestData.UNDEPLOYED_APP;
import static com.nix.flakedrift.drift.infra.WorkspaceGraphServiceTestData.UNDEPLOYED_APP_DIR;
import static com.nix.flakedrift.drift.infra.WorkspaceGraphServiceTestData.UNDEPLOYED_APP_HASH;
import static com.nix.flakedrift.drift.infra.WorkspaceGraphServiceTestData.UNDEPLOYED_FIXTURE;
import static com.nix.flakedrift.drift.infra.WorkspaceGraphServiceTestData.UPSTREAM;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Builds graphs over flake fixtures; nix hashing is mocked (e.g. no /nix/store needed). */
class WorkspaceGraphServiceTests {

    @Test
    void givenDriftTreeFixture_whenBuildingGraph_thenShapeMatchesRoles() throws URISyntaxException {
        Path rootDir = fixture(FIXTURE_ROOT);
        FlakeGraphNode root = buildRoot(rootDir);

        assertEquals("root", root.getName());
        assertEquals(rootDir, root.getPath());

        // root inputs: upstream + home-manager (remote) + aggregator (path, no narHash)
        assertEquals(List.of(UPSTREAM, HOME_MANAGER, AGGREGATOR), names(root));
        assertNull(child(root, UPSTREAM).getPath());
        assertNull(child(root, UPSTREAM).getLockHash());
        assertNull(child(root, HOME_MANAGER).getPath());

        FlakeGraphNode aggregator = child(root, AGGREGATOR);
        assertEquals(rootDir.resolve(AGGREGATOR), aggregator.getPath());
        assertNull(aggregator.getLockHash(), "path-only entry in the machine lock -> no narHash");

        assertEquals(List.of(UPSTREAM, LEAF_MODULE, HUB_FLAKE), names(aggregator));

        FlakeGraphNode leafModule = child(aggregator, LEAF_MODULE);
        assertEquals(LEAF_MODULE_HASH, leafModule.getLockHash());
        assertEquals(rootDir.resolve(AGGREGATOR + "/" + LEAF_MODULE), leafModule.getPath());
        assertEquals(List.of(UPSTREAM), names(leafModule));

        FlakeGraphNode hubFlake = child(aggregator, HUB_FLAKE);
        assertEquals(HUB_FLAKE_HASH, hubFlake.getLockHash());
        assertEquals(List.of(UPSTREAM, REMOTE_INPUT, LEAF_APP), names(hubFlake));
        assertNull(child(hubFlake, REMOTE_INPUT).getPath());

        FlakeGraphNode leafApp = child(hubFlake, LEAF_APP);
        assertEquals(LEAF_APP_HASH, leafApp.getLockHash());
        assertEquals(rootDir.resolve(AGGREGATOR + "/" + HUB_FLAKE + "/" + LEAF_APP), leafApp.getPath());
    }

    @Test
    void givenCleanFixture_whenBuildingGraph_thenCleanTreeShape() throws URISyntaxException {
        Path rootDir = fixture(CLEAN_FIXTURE);
        FlakeGraphNode root = buildRoot(rootDir);

        assertEquals(List.of(CLEAN_UPSTREAM_1, CLEAN_APP, CLEAN_SERVICE), names(root));
        assertNull(child(root, CLEAN_UPSTREAM_1).getPath());

        FlakeGraphNode app = child(root, CLEAN_APP);
        assertEquals(CLEAN_APP_HASH, app.getLockHash());
        assertEquals(rootDir.resolve(CLEAN_APP_DIR), app.getPath());

        FlakeGraphNode service = child(root, CLEAN_SERVICE);
        assertEquals(CLEAN_SERVICE_HASH, service.getLockHash());
        assertEquals(rootDir.resolve(CLEAN_SERVICE_DIR), service.getPath());
    }

    @Test
    void givenUndeployedFixture_whenBuildingGraph_thenSingleAppLeaf() throws URISyntaxException {
        Path rootDir = fixture(UNDEPLOYED_FIXTURE);
        FlakeGraphNode root = buildRoot(rootDir);

        assertEquals(List.of(UNDEPLOYED_APP), names(root));

        FlakeGraphNode app = child(root, UNDEPLOYED_APP);
        assertEquals(UNDEPLOYED_APP_HASH, app.getLockHash());
        assertEquals(rootDir.resolve(UNDEPLOYED_APP_DIR), app.getPath());
        assertTrue(app.getChildren().isEmpty(), "app is a leaf with no nested inputs");
    }

    @Test
    void givenDeepChainFixture_whenBuildingGraph_thenNestedChain() throws URISyntaxException {
        Path rootDir = fixture(DEEP_CHAIN_FIXTURE);
        FlakeGraphNode root = buildRoot(rootDir);

        FlakeGraphNode layer1 = child(root, DEEP_LAYER_1);
        assertEquals(DEEP_LAYER1_HASH, layer1.getLockHash());

        FlakeGraphNode layer2 = child(layer1, DEEP_LAYER_2);
        assertEquals(DEEP_LAYER2_HASH, layer2.getLockHash());

        FlakeGraphNode layer3 = child(layer2, DEEP_LAYER_3);
        assertEquals(DEEP_LAYER3_HASH, layer3.getLockHash());
        assertEquals(rootDir.resolve(DEEP_LAYER_1_DIR), layer1.getPath());
        assertEquals(rootDir.resolve(DEEP_LAYER_1_DIR + "/" + DEEP_LAYER_2_DIR), layer2.getPath());
        assertEquals(rootDir.resolve(DEEP_LAYER_1_DIR + "/" + DEEP_LAYER_2_DIR + "/" + DEEP_LAYER_3_DIR), layer3.getPath());
    }

    @Test
    void givenMultiCauseFixture_whenBuildingGraph_thenHubWithTwoDriftedLeaves() throws URISyntaxException {
        Path rootDir = fixture(MULTI_CAUSE_FIXTURE);
        FlakeGraphNode root = buildRoot(rootDir);

        FlakeGraphNode hub = child(root, MULTI_HUB);
        assertEquals(MULTI_HUB_HASH, hub.getLockHash());
        assertEquals(List.of(MULTI_LEAF_A, MULTI_LAYER), names(hub));

        FlakeGraphNode leafA = child(hub, MULTI_LEAF_A);
        assertEquals(MULTI_LEAF_A_HASH, leafA.getLockHash());
        assertTrue(leafA.getChildren().isEmpty());

        FlakeGraphNode layer = child(hub, MULTI_LAYER);
        assertEquals(MULTI_LAYER_HASH, layer.getLockHash());
    }

    @Test
    void givenStagingMachineFixture_whenBuildingGraph_thenRootBoardsAggregatorBoardingAllModules() throws URISyntaxException {
        Path rootDir = fixture(STAGING_FIXTURE);
        FlakeGraphNode root = buildRoot(rootDir);

        assertEquals(STAGING_ROOT, root.getName());
        assertEquals(rootDir, root.getPath());

        // root carries exactly one local path input: the aggregator.
        assertEquals(List.of(STAGING_AGGREGATOR), names(root));
        FlakeGraphNode aggregator = child(root, STAGING_AGGREGATOR);
        assertEquals(rootDir.resolve(STAGING_AGGREGATOR), aggregator.getPath());

        // The aggregator boards every other module flake as a local path input.
        List<FlakeGraphNode> modules = aggregator.getChildren();
        assertEquals(STAGING_AGGREGATOR_INPUTS, modules.size());
        assertTrue(modules.stream().allMatch(m -> m.getPath() != null),
                "every module must be a local path input");
        assertTrue(modules.stream().allMatch(m -> m.getName().matches("flake-\\d+")),
                "module inputs must be named after their flake dirs");
        assertEquals(STAGING_AGGREGATOR_INPUTS, modules.stream()
                .map(FlakeGraphNode::getName).distinct().count(),
                "each module must reference a distinct flake dir");

        // Modules are leaves except flake-04 which nests flake-03; every module
        // (and the nested flake-03) carries a remote nixpkgs input. Enforce the
        // tree stays shallow: all grandchildren (if any) are leaves.
        assertTrue(modules.stream().allMatch(m ->
                        m.getChildren().stream().allMatch(c ->
                                c.getChildren().isEmpty()
                                        || c.getChildren().stream().allMatch(g -> g.getChildren().isEmpty()))),
                "modules must nest at most one level plus remote nixpkgs leaves");
        // 115 nodes: root + flake-01 + 56 modules + nested flake-03 + 56 remote nixpkgs.
        assertEquals(115, countNodes(root),
                "root + aggregator + 56 modules + nested flake-03 + nixpkgs remotes expected");
    }

    @Test
    void givenPathInputToMissingDir_whenBuildingGraph_thenLeafHasNoChildren(@TempDir Path dir) throws IOException {
        String missingInputName = "ghost";
        String missingInputLockHash = "sha256-ghost";
        Path missingInputPath = dir.resolve("does-not-exist");
        write(dir.resolve("flake.nix"), """
                {
                  inputs = {
                    upstream.url = "github:example/upstream/main";
                    ghost = { url = "path:./does-not-exist"; };
                  };
                }
                """);
        write(dir.resolve("flake.lock"), """
                {
                  "nodes": {
                    "root": { "inputs": { "ghost": "ghost", "upstream": "upstream" } },
                    "ghost": {
                      "locked": { "narHash": "sha256-ghost", "path": "./does-not-exist", "type": "path" }
                    },
                    "upstream": {
                      "locked": { "narHash": "sha256-upstream-remote", "rev": "abc", "type": "github" }
                    }
                  },
                  "root": "root",
                  "version": 7
                }
                """);

        IWorkspaceGraphService service = new WorkspaceGraphServiceTestBuilder().build();
        FlakeGraphNode ghost = child(service.buildDependencyGraph(dir).getRoot(), missingInputName);
        assertEquals(missingInputPath, ghost.getPath());
        assertEquals(missingInputLockHash, ghost.getLockHash());
        assertTrue(ghost.getChildren().isEmpty(), "walk stops when the target has no flake.nix");
    }

    private static Path fixture(String root) throws URISyntaxException {
        return Path.of(WorkspaceGraphServiceTests.class.getClassLoader().getResource(root).toURI());
    }

    private static FlakeGraphNode buildRoot(Path rootDir) {
        return new WorkspaceGraphServiceTestBuilder()
                .withSetupNixCommandMock()
                .build()
                .buildDependencyGraph(rootDir).getRoot();
    }

    private static void write(Path path, String content) throws IOException {
        Files.writeString(path, content);
    }

    private static List<String> names(FlakeGraphNode node) {
        return node.getChildren().stream().map(FlakeGraphNode::getName).toList();
    }

    private static int countNodes(FlakeGraphNode node) {
        int count = 1;
        for (FlakeGraphNode c : node.getChildren()) {
            count += countNodes(c);
        }
        return count;
    }

    private static FlakeGraphNode child(FlakeGraphNode node, String name) {
        return node.getChildren().stream()
                .filter(c -> c.getName().equals(name))
                .findFirst()
                .orElseThrow(() -> new AssertionError("no child '" + name + "' under " + node.getName()));
    }
}
