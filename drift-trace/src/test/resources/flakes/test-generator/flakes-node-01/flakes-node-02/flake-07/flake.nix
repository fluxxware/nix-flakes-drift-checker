# GENERIC TEST FIXTURE — no real machine data
{
  description = "Generic flake (flake-07)";

  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs/nixos-unstable";
  };

  outputs =
    { self, ... }:
    {
      packages.x86_64-linux.default = { };
    };
}
