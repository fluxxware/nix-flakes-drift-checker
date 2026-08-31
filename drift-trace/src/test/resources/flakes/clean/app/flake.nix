# Fixture: app module under the clean machine.
{
  description = "App module (clean fixture)";

  inputs = {
    upstream-flake-2.url = "github:example/upstream/main";
  };

  outputs = { self, upstream-flake-2, ... }: { };
}
