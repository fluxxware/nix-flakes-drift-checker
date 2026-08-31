# shell.nix
{ pkgs ? import <nixpkgs> {} }:

pkgs.mkShell {
  name = "nix-drift-dev";

  buildInputs = with pkgs; [
    jdk21
    gradle
    graalvm-ce
  ];

  # Auto-set JAVA_HOME and tell Gradle to use it
  shellHook = ''
    export JAVA_HOME=${pkgs.jdk21.home}
    export PATH=${pkgs.jdk21}/bin:$PATH

    # Force Gradle to use this JDK (even with wrapper)
    mkdir -p .gradle
    echo "org.gradle.java.home=$JAVA_HOME" > .gradle/gradle.properties

    echo "nix-drift dev shell ready!"
    echo "JAVA_HOME = $JAVA_HOME"
    echo "Gradle version:"
    gradle --version | grep Gradle
  '';
}
