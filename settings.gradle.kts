// NIX-BUILD NOTE: repository declarations are centralized HERE on purpose.
// The `nix build` path (flake.nix → buildGradleApplication) rewrites repository
// declarations via an init script, so it must find a single points of truth in
// this file (Root #4 of raphiz/buildGradleApplication). Do NOT sprinkle repos
// in build.gradle.kts files again.
pluginManagement {
    repositories {
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
    // Fail loudly if any project re-declares its own repositories.
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
}

rootProject.name = "nix-flakes-drift-checker"

include("drift-trace", "app")