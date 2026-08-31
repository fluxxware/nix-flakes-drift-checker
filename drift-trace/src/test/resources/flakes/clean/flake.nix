# Fixture: clean machine — everything in sync.
{
  description = "Clean machine (fixture)";

  inputs = {
    upstream-flake-1.url = "github:example/upstream/main";
    app-flake-1 = { url = "path:./app"; };
    service-flake-1 = { url = "path:./service"; };
  };

  outputs = { self, upstream-flake-1, app-flake-1, service-flake-1, ... }: { };
}
