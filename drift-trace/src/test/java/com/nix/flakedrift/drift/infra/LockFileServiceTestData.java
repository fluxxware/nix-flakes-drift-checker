package com.nix.flakedrift.drift.infra;

/** Shared flake.lock bodies for {@link LockFileServiceTests}. */
public final class LockFileServiceTestData {
    private LockFileServiceTestData() {
    }

    public static final String WITH_NARHASH = """
            {
              "nodes": {
                "root": { "inputs": { "leaf-app": "leaf-app" } },
                "leaf-app": {
                  "inputs": { "nixpkgs": "nixpkgs" },
                  "locked": {
                    "lastModified": 1786705083,
                    "narHash": "sha256-Kt3+iZEzLAQaMVzW+hcNmZX9VlaBqxYaDLzb7V6+SkU=",
                    "path": "/fixtures/leaf-app",
                    "type": "path"
                  }
                }
              },
              "root": "root",
              "version": 7
            }
            """;
    public static final String PATH_ONLY = """
            {
              "nodes": {
                "aggregator": {
                  "locked": { "path": "./aggregator", "type": "path" }
                }
              },
              "root": "root",
              "version": 7
            }
            """;
    public static final String GITHUB = """
            {
              "nodes": {
                "nixpkgs": {
                  "locked": {
                    "lastModified": 1751234567,
                    "narHash": "sha256-aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa=",
                    "rev": "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
                    "type": "github"
                  }
                }
              },
              "root": "root",
              "version": 7
            }
            """;
    public static final String MALFORMED = "{ not json";
}
