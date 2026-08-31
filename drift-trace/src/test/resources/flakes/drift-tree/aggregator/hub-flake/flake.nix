# Fixture: a hub flake owning a nested leaf and a remote input.
{
  description = "Hub flake (fixture)";

  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs/c8aa8cc00a5cb57fada0851a038d35c08a36a2b";
    remote-input = {
      url = "github:example/example-app/main";
      inputs.nixpkgs.follows = "nixpkgs";
    };
    leaf-app.url = "path:./leaf-app";
  };

  outputs = { self, nixpkgs, remote-input, leaf-app, ... }: { };
}
