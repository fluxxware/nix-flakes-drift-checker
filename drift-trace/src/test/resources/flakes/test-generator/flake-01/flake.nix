# GENERIC TEST FIXTURE — no real machine data
{
  description = "Generic flake (flake-01)";

  inputs = {
    flake-02.url = "path:../flakes-node-01/flakes-node-02/flake-02";
    flake-20.url = "path:../flakes-node-01/flakes-node-02/flake-20";
    flake-09.url = "path:../flakes-node-01/flakes-node-02/flake-09";
    flake-14.url = "path:../flakes-node-01/flakes-node-02/flake-14";
    flake-21.url = "path:../flakes-node-01/flakes-node-02/flake-21";
    flake-35.url = "path:../flakes-node-01/flakes-node-02/flake-03/flake-35";
    flake-12.url = "path:../flakes-node-01/flakes-node-02/flake-12";
    flake-06.url = "path:../flakes-node-01/flakes-node-02/flake-06";
    flake-11.url = "path:../flakes-node-01/flakes-node-02/flake-11";
    flake-08.url = "path:../flakes-node-01/flakes-node-02/flake-08";
    flake-34.url = "path:../flakes-node-01/flakes-node-02/flake-03/flake-34";
    flake-04.url = "path:../flakes-node-01/flakes-node-02/flake-04";
    flake-13.url = "path:../flakes-node-01/flakes-node-02/flake-13";
    flake-39.url = "path:../flakes-node-01/flakes-node-02/flakes-node-04/flake-39";
    flake-58.url = "path:../flakes-node-01/flakes-node-02/flakes-node-04/flakes-node-05/flake-58";
    flake-37.url = "path:../flakes-node-01/flakes-node-02/flakes-node-04/flake-37";
    flake-38.url = "path:../flakes-node-01/flakes-node-02/flakes-node-04/flake-38";
    flake-10.url = "path:../flakes-node-01/flakes-node-02/flake-10";
    flake-19.url = "path:../flakes-node-01/flakes-node-02/flake-19";
    flake-15.url = "path:../flakes-node-01/flakes-node-02/flake-15";
    flake-16.url = "path:../flakes-node-01/flakes-node-02/flake-16";
    flake-25.url = "path:../flakes-node-01/flakes-node-15/flake-25";
    flake-31.url = "path:../flakes-node-01/flakes-node-15/flake-31";
    flake-23.url = "path:../flakes-node-01/flakes-node-15/flake-23";
    flake-26.url = "path:../flakes-node-01/flakes-node-15/flake-26";
    flake-27.url = "path:../flakes-node-01/flakes-node-15/flake-27";
    flake-29.url = "path:../flakes-node-01/flakes-node-15/flake-29";
    flake-28.url = "path:../flakes-node-01/flakes-node-15/flake-28";
    flake-30.url = "path:../flakes-node-01/flakes-node-15/flake-30";
    flake-22.url = "path:../flakes-node-01/flakes-node-15/flake-22";
    flake-07.url = "path:../flakes-node-01/flakes-node-02/flake-07";
    flake-44.url = "path:../flakes-node-01/flakes-node-02/flakes-node-07/flake-44";
    flake-43.url = "path:../flakes-node-01/flakes-node-02/flakes-node-07/flake-43";
    flake-41.url = "path:../flakes-node-01/flakes-node-02/flakes-node-07/flake-41";
    flake-45.url = "path:../flakes-node-01/flakes-node-02/flakes-node-07/flake-45";
    flake-42.url = "path:../flakes-node-01/flakes-node-02/flakes-node-07/flake-42";
    flake-52.url = "path:../flakes-node-01/flakes-node-02/flakes-node-13/flake-52";
    flake-51.url = "path:../flakes-node-01/flakes-node-02/flakes-node-12/flake-51";
    flake-48.url = "path:../flakes-node-01/flakes-node-02/flakes-node-09/flake-48";
    flake-47.url = "path:../flakes-node-01/flakes-node-02/flakes-node-09/flake-47";
    flake-50.url = "path:../flakes-node-01/flakes-node-02/flakes-node-11/flake-50";
    flake-46.url = "path:../flakes-node-01/flakes-node-02/flakes-node-08/flake-46";
    flake-36.url = "path:../flakes-node-01/flakes-node-02/flakes-node-03/flake-36";
    flake-57.url = "path:../flakes-node-01/flakes-node-02/flakes-node-14/flake-57";
    flake-56.url = "path:../flakes-node-01/flakes-node-02/flakes-node-14/flake-56";
    flake-53.url = "path:../flakes-node-01/flakes-node-02/flakes-node-14/flake-53";
    flake-54.url = "path:../flakes-node-01/flakes-node-02/flakes-node-14/flake-54";
    flake-55.url = "path:../flakes-node-01/flakes-node-02/flakes-node-14/flake-55";
    flake-49.url = "path:../flakes-node-01/flakes-node-02/flakes-node-10/flake-49";
    flake-40.url = "path:../flakes-node-01/flakes-node-02/flakes-node-06/flake-40";
    flake-05.url = "path:../flakes-node-01/flakes-node-02/flake-05";
    flake-17.url = "path:../flakes-node-01/flakes-node-02/flake-17";
    flake-24.url = "path:../flakes-node-01/flakes-node-15/flake-24";
    flake-18.url = "path:../flakes-node-01/flakes-node-02/flake-18";
    flake-32.url = "path:../flakes-node-01/flakes-node-02/flake-03/flake-32";
    flake-33.url = "path:../flakes-node-01/flakes-node-02/flake-03/flake-33";
  };

  outputs =
    { self, ... }:
    {
      packages.x86_64-linux.default = { };
    };
}
