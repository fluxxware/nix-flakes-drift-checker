# Fixture: layer-1 in the deep chain.
{
  description = "Layer 1 (deep chain fixture)";

  inputs = {
    layer-flake-2 = { url = "path:./layer-2"; };
  };

  outputs = { self, layer-flake-2, ... }: { };
}
