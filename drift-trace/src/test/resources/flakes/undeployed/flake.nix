# Fixture: undeployed machine — an app locked but never realized in the store.
{
  description = "Undeployed machine (fixture)";

  inputs = {
    app-flake-1 = { url = "path:./app"; };
  };

  outputs = { self, app-flake-1, ... }: { };
}
