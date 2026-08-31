# Fixture: a leaf app depending on nixpkgs only.
{
  description = "Leaf app (fixture)";

  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs/c8aa8cc00a5cb57fada0851a038d35c08a36a2b";
  };

  outputs = { self, nixpkgs, ... }: { };
}
