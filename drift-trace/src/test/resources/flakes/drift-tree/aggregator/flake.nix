# Fixture: an aggregator flake that gathers nested modules.
{
  description = "Aggregator flake (fixture)";

  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs/c8aa8cc00a5cb57fada0851a038d35c08a36a2b";
    leaf-module = {
      url = "path:./leaf-module";
    };
    hub-flake = {
      url = "path:./hub-flake";
    };
  };

  outputs = { self, nixpkgs, leaf-module, hub-flake, ... }: { };
}
