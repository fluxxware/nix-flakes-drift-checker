package com.nix.flakedrift.drift.infra;

/** Shared flake.nix bodies for {@link FlakeDefinitionServiceTests}. */
public final class FlakeDefinitionServiceTestData {
    private FlakeDefinitionServiceTestData() {
    }

    public static final String SIMPLE = """
            {
              inputs = {
                nixpkgs.url = "github:NixOS/nixpkgs/nixos-25.11";
                home-manager = { url = "github:nix-community/home-manager"; };
                aggregator = { url = "path:./aggregator"; };
              };
            }
            """;
    public static final String FOLLOWS = """
            {
              inputs = {
                nixpkgs.url = "github:NixOS/nixpkgs/nixos-25.11";
                home-manager = {
                  url = "github:nix-community/home-manager";
                  inputs.nixpkgs.follows = "nixpkgs";
                };
              };
            }
            """;
    public static final String ABSOLUTE_PATH = """
            {
              inputs = {
                leaf-module = { url = "path:/fixtures/leaf-module"; };
              };
            }
            """;
    public static final String FALSE_POSITIVES = """
            {
              description = "input { url = \\"path:./false-1\\"; } keeps balance";
              inputs = {
                # { url = "path:./false-2"; }
                nixpkgs.url = "github:NixOS/nixpkgs";
                app = { url = "path:./app"; };
              };
            }
            """;
    public static final String OUTSIDE_BLOCK = """
            {
              inputs = { aggregator = { url = "path:./aggregator"; }; };
              outputs = { self, ... }@inputs:
                let
                  notAnInput = { url = "path:./false-positive"; };
                in { };
            }
            """;
}
