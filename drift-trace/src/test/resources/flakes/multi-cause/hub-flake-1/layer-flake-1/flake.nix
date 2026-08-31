# Fixture: a layer under the hub, itself carrying a drifted leaf (branch b).
{
  description = "Layer (multi-cause fixture)";

  inputs = {
    leaf-flake-2 = { url = "path:./leaf-flake-2"; };
  };

  outputs = { self, leaf-flake-2, ... }: { };
}
