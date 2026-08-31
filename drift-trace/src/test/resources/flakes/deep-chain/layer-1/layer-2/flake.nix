# Fixture: layer-2 in the deep chain.
{
  description = "Layer 2 (deep chain fixture)";

  inputs = {
    layer-flake-3 = { url = "path:./layer-3"; };
  };

  outputs = { self, layer-flake-3, ... }: { };
}
