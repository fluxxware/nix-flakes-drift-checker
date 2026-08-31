package com.nix.flakedrift.drift.infra;

/** Shared fixture data for {@link WorkspaceGraphServiceTests}. */
public final class WorkspaceGraphServiceTestData {
    private WorkspaceGraphServiceTestData() {
    }

    /** Resource root of the nested drift-tree fixture (original role-based tree). */
    public static final String FIXTURE_ROOT = "flakes/drift-tree";

    /** The drift-tree fixture mirrors: root -> aggregator -> {leaf-module, hub-flake -> {leaf-app}}. */
    public static final String ROOT_INPUT = "root";
    public static final String AGGREGATOR = "aggregator";
    public static final String LEAF_MODULE = "leaf-module";
    public static final String HUB_FLAKE = "hub-flake";
    public static final String LEAF_APP = "leaf-app";
    public static final String REMOTE_INPUT = "remote-input";
    public static final String UPSTREAM = "nixpkgs";
    public static final String HOME_MANAGER = "home-manager";

    /** Realistic (but arbitrary) narHashes pinned by the fixture locks. */
    public static final String LEAF_MODULE_HASH =
            "sha256-gBSHSEOg8erAvui0MJTHaJAx3we5PlWxx56T7lW2Rsw=";
    public static final String HUB_FLAKE_HASH =
            "sha256-k2opEK6f6cwqYd31WlxcgnRU8bZw6IRw+fFvF43ZVaI=";
    public static final String LEAF_APP_HASH =
            "sha256-Kt3+iZEzLAQaMVzW+hcNmZX9VlaBqxYaDLzb7V6+SkU=";

    // ---- dataset-derived fixtures ----

    /** Resource root of the clean fixture: root -> {upstream(app remote), app, service}. */
    public static final String CLEAN_FIXTURE = "flakes/clean";
    public static final String CLEAN_APP_DIR = "app";
    public static final String CLEAN_SERVICE_DIR = "service";
    public static final String CLEAN_ROOT = "root";
    public static final String CLEAN_UPSTREAM_1 = "upstream-flake-1";
    public static final String CLEAN_APP = "app-flake-1";
    public static final String CLEAN_SERVICE = "service-flake-1";
    public static final String CLEAN_APP_HASH = "sha256-app-sync";
    public static final String CLEAN_SERVICE_HASH = "sha256-service-sync";

    /** Resource root of the undeployed fixture: root -> app (locked, no store). */
    public static final String UNDEPLOYED_FIXTURE = "flakes/undeployed";
    public static final String UNDEPLOYED_APP_DIR = "app";
    public static final String UNDEPLOYED_APP = "app-flake-1";
    public static final String UNDEPLOYED_APP_HASH = "sha256-app-deployed";

    /** Resource root of the deep-chain fixture: root -> layer1 -> layer2 -> layer3 -> leaf. */
    public static final String DEEP_CHAIN_FIXTURE = "flakes/deep-chain";
    public static final String DEEP_LAYER_1 = "layer-flake-1";
    public static final String DEEP_LAYER_2 = "layer-flake-2";
    public static final String DEEP_LAYER_3 = "layer-flake-3";
    public static final String DEEP_LAYER_1_DIR = "layer-1";
    public static final String DEEP_LAYER_2_DIR = "layer-2";
    public static final String DEEP_LAYER_3_DIR = "layer-3";
    public static final String DEEP_LAYER1_HASH = "sha256-layer-1";
    public static final String DEEP_LAYER2_HASH = "sha256-layer-2";
    public static final String DEEP_LAYER3_HASH = "sha256-layer-3";

    /** Resource root of the multi-cause fixture: root -> hub -> {leaf-a, layer -> leaf-b}. */
    public static final String MULTI_CAUSE_FIXTURE = "flakes/multi-cause";
    public static final String MULTI_HUB = "hub-flake-1";
    public static final String MULTI_LEAF_A = "leaf-flake-1";
    public static final String MULTI_LAYER = "layer-flake-1";
    public static final String MULTI_HUB_HASH = "sha256-hub";
    public static final String MULTI_LEAF_A_HASH = "sha256-leaf-a-lock";
    public static final String MULTI_LAYER_HASH = "sha256-sub";

    /** Resource root of the large staging-machine fixture (fully generic test tree). */
    public static final String STAGING_FIXTURE = "flakes/staging-machine";
    public static final String STAGING_ROOT = "root";
    /** The root flake.nix carries a single path input named after its flake dir (flake-01). */
    public static final String STAGING_AGGREGATOR = "flake-01";
    /** The aggregator boards every other module flake as an input. */
    public static final int STAGING_AGGREGATOR_INPUTS = 56;
}
