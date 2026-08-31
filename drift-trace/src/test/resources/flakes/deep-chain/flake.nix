# Fixture: a deep stale chain — nested path inputs down to a drifted leaf.
{
  description = "Deep chain machine (fixture)";

  inputs = {
    layer-flake-1 = { url = "path:./layer-1"; };
  };

  outputs = { self, layer-flake-1, ... }: { };
}
