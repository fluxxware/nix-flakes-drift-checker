# Fixture: hub with two branches, each with a drifted leaf.
{
  description = "Hub flake (multi-cause fixture)";

  inputs = {
    leaf-flake-1 = { url = "path:./leaf-flake-1"; };
    layer-flake-1 = { url = "path:./layer-flake-1"; };
  };

  outputs = { self, leaf-flake-1, layer-flake-1, ... }: { };
}
