# GENERIC TEST FIXTURE — no real machine data
{
  description = "Generic flake (staging-machine)";

  inputs = {
    flake-01.url = "path:flake-01";
  };

  outputs =
    { self, ... }:
    {
      packages.x86_64-linux.default = { };
    };
}
