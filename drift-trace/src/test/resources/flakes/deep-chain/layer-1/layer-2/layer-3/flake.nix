# Fixture: layer-3 in the deep chain.
{
  description = "Layer 3 (deep chain fixture)";

  inputs = {
    leaf-flake-1 = { url = "path:./leaf-flake-1"; };
  };

  outputs = { self, leaf-flake-1, ... }: { };
}
