# NIX-BUILD (this file is the entire nix packaging story — read from the bottom up).
#
# 1. `packages.x86_64-linux.default` — a pure, sandbox-safe build of the GraalVM
#    NATIVE image of `flakes-drift-checker` via raphiz/buildGradleApplication:
#      - ALL maven/gradle-plugin deps are prefetched by buildGradleApplication into
#        an offline maven repo, keyed by hashes from gradle/verification-metadata.xml
#        (generated once with
#         `./gradlew --refresh-dependencies --write-verification-metadata sha256 dependencies`).
#      - Gradle 9.6.0 (exactly the wrapper version) is built from the wrapper's
#        distributionUrl + distributionSha256Sum via gradleFromWrapper.
#      - `gradle :app:nativeCompile` runs OFFLINE inside the sandbox (no network).
# 2. `devShells.x86_64-linux.default` — same dev env as ./shell.nix.
# 3. `apps.x86_64-linux.default` — makes `nix run . -- update ...` work.
{
  description = "flakes-drift-checker — drift detection + multi-flake update CLI (GraalVM native image)";

  inputs = {
    # 25.11 == the JDK/gradle-generation used across the local /etc/nixos flakes.
    nixpkgs.url = "github:NixOS/nixpkgs/nixos-25.11";
    # pure Gradle packaging: verification-metadata.xml lockfile + offline maven repo.
    build-gradle-application.url = "github:raphiz/buildGradleApplication";
  };

  outputs = { self, nixpkgs, build-gradle-application, ... }:
    let
      system = "x86_64-linux";
      overlay = build-gradle-application.overlays.default;
      pkgs = import nixpkgs { inherit system; overlays = [ overlay ]; };

      # Exactly the gradle version pinned by ./gradle/wrapper/gradle-wrapper.properties.
      gradle = pkgs.gradleFromWrapper {
        wrapperPropertiesPath = ./gradle/wrapper/gradle-wrapper.properties;
        defaultJava = pkgs.jdk21;
      };

      # NIX-BUILD NOTE: version comes from the SINGLE source — root gradle.properties
      # (`version=`), same value that gradle stamps into the jar/version.properties.
      # No duplicate version literal in flake.nix.
      version = let
        props = builtins.readFile ./gradle.properties;
        m = builtins.match ".*version=([^\n]*).*" props;
      in
        if m == null then "unknown" else builtins.head m;

      # The GraalVM JDK whose `native-image` builds the binary. GRAALVM_HOME is
      # exported into the build env (buildGradleApplication merges `env` into the
      # derivation). The plugin's reachability-metadata download is disabled in
      # app/build.gradle.kts (metadataRepository.enabled = false) so the sandbox
      # build needs no network beyond the prefetched maven repo.
      graalvm = pkgs.graalvmPackages.graalvm-ce;

      # NIX-BUILD NOTE: buildGradleApplication's default installPhase expects a JVM
      # distribution (lib/*.jar + bin/*). We build the NATIVE image instead, so the
      # install phase is overridden to ship the single produced binary.
      # NIX-BUILD NOTE: gitignore-aware source snapshot — respects ./gitignore, so
      # build outputs, .gradle caches and IDE state never reach the sandbox build.
      # (pkgs.nix-gitignore.gitignoreSource is the nixpkgs built-in equivalent of
      # lib.gitignoreSource that this nixpkgs revision does not expose on lib.)
      src = pkgs.nix-gitignore.gitignoreSource [ ./.gitignore ] ./.;
      nativeImage = (pkgs.buildGradleApplication {
        pname = "flakes-drift-checker";
        inherit version;
        inherit src;
        jdk = pkgs.jdk21;
        inherit gradle;
        buildTask = ":app:nativeCompile";
        # glibc in buildInputs -> lands in the output closure: the native image is
        # dynamically linked against the build glibc (ldd confirms libc/ld-linux).
        buildInputs = [ pkgs.glibc ];
        nativeBuildInputs = [
          graalvm
          pkgs.gcc
          pkgs.glibc.dev
        ];
        env.GRAALVM_HOME = "${graalvm}";
      }).overrideAttrs (old: {
        installPhase = ''
          runHook preInstall
          mkdir -p $out/bin
          install -m 755 app/build/native/nativeCompile/flakes-drift-checker $out/bin/flakes-drift-checker
          runHook postInstall
        '';
        postFixup = "";
      });
    in
    {
      packages.${system}.default = nativeImage;

      apps.${system}.default = {
        type = "app";
        program = "${nativeImage}/bin/flakes-drift-checker";
      };

      devShells.${system}.default = import ./shell.nix;
    };
}
