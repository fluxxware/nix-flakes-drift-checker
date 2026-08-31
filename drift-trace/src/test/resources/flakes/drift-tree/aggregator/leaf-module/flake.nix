# Fixture: a leaf module depending on nixpkgs only.
{
  description = "Leaf module (fixture)";

  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs/c8aa8cc00a5cb57fada0851a038d35c08a36a2b";
  };

  outputs = { self, nixpkgs, ... }: { };
}
