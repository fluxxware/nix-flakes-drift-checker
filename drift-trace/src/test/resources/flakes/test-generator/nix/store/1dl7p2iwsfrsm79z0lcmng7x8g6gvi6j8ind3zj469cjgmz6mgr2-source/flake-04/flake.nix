# GENERIC TEST FIXTURE — no real machine data
{
  description = "Generic flake (flake-04)";

  inputs = {
    flake-03.url = "path:../flake-03";
  };

  outputs =
    { self, ... }:
    {
      packages.x86_64-linux.default = { };
    };
}
