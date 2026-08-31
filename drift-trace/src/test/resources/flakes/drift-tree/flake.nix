# Fixture: a machine flake that aggregates nested flake inputs.
{
  description = "Machine flake (fixture)";

  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs/c8aa8cc00a5cb57fada0851a038d35c08a36a2b";
    home-manager = { url = "github:nix-community/home-manager/release-25.05"; inputs.nixpkgs.follows = "nixpkgs"; };
    aggregator = { url = "path:./aggregator"; };
  };

  outputs = { self, nixpkgs, home-manager, aggregator, ... }:
    let
      system = "x86_64-linux";
    in
    {
      nixosConfigurations.nixos = nixpkgs.lib.nixosSystem {
        inherit system;
        modules = [ aggregator.nixosModules.default ];
      };
    };
}
