# Fixture: service module under the clean machine.
{
  description = "Service module (clean fixture)";

  inputs = {
    upstream-flake-3.url = "github:example/upstream/main";
  };

  outputs = { self, upstream-flake-3, ... }: { };
}
