# Fixture: multi-cause machine — a hub with two independently drifted leaves.
{
  description = "Multi-cause machine (fixture)";

  inputs = {
    hub-flake-1 = { url = "path:./hub-flake-1"; };
  };

  outputs = { self, hub-flake-1, ... }: { };
}
