# What is it 

A diagnostic opinionated utility that helps you manage system state and verify whether your actual deployment
matches your flakes "codebase" content, although it comes with extra features for quality of life.
This utility is made for developers and system administrators who fall into one of the following categories:

1. **Tired of casino betting with `nix flake update`**
   In a monolithic setup, a single flawed library or fresh package in a third-party input breaks
   your entire system build. This tool updates the tree from the bottom up, looking at deepest level first flakes,
   so a problem stays local to one microflake instead of sending you spelunking through thousands
   of lines of spaghetti.
2. **Sick of spawning fake Git repositories just for local dependencies**
   Orthodox Nix wants every piece to be its own repository with git hashes. This lets you keep a
   clean component architecture built on local relative paths, so you have no commit bureaucracy, no loss of
   strictness.
3. **Deploying diverse hardware from a single root**
   A NixOS desktop today, a cloud server tomorrow, a home router (or a non-Nix system
   via standalone Home Manager), it won't matter cause a monolith makes that migration hell. Microflakes with automatic
   drift tracking let you compose the building blocks you need (e.g. `composition roots`) without
   cross-contaminating contexts. And when you do update, the built-in history logs what changed,
   when, and how. Metrics, yay!

## Target Audience

You will actually benefit from this tool if:
- you use microflakes and want to shed rigid monolithic flake designs,
- you manage multiple deployments (desktop, server, cloud, IoT/router) from one codebase,
- your environment is a monorepo with a deep flake nesting level,
- you want a no-overhead local loop where relative paths just work, so you won't have half-baked
  commits to remote repos just to test a one-line change.
- you want to try microflakes - this thing will save you plenty of time

It automatically detects, tracks, and fixes dependency and version drifts across multi-module or composite flake structures where standard lock files fall short. 
This thing ensures absolute determinism without requiring every local microflake to be an isolated Git repository.

# Overview
This command will print you and overview 
```bash
flakes-drift-checker
```
Available options for its usage are:

| Command   | Flag                    | Meaning                                | Default          |
|-----------|-------------------------|----------------------------------------|------------------|
| `check`   | `--root=<path>`         | Flake root directory to scan           | /etc/nixos       |
| `check`   | `--json`                | Machine-readable JSON report           | —                |
| `update`  | `--root=<path>`         | Flake root directory to scan           | /etc/nixos       |
| `update`  | `--dry-run`             | Print the update chain, update nothing | —                |
| `update`  | `--history=<path>`      | Update-history directory override      | `~/.flake-drift` |
| `history` | `--last=<N>`            | Show the last N runs                   | 10               |
| `history` | `--flake=<name>`        | Only runs touching that flake          | —                |
| `history` | `--json`                | Dump raw history JSON                  | —                |
| `history` | `--history=<path>`      | Update-history directory override      | `~/.flake-drift` |

Every update run appends a per-run record to `~/.flake-drift/<timestamp>.json` :
```json
{
"timestamp": "2026-08-27T22:32:38.503557969Z",
"flakes": [
{ "name": "root", "depth": 0, "changed": true,
"lockBefore": "sha256-o9pD6b…", "lockAfter": "sha256-hcCVq…", "nixOutput": "" }
],
"summary": { "total": 5, "changed": 3, "unchanged": 2 }
}
```
# Running options

### running help
```bash
flakes-drift-checker --help
```
### Updating actual flakes (default path option is `/etc/nixos`)
```bash
flakes-drift-checker --update
```
### Updating with your root flake in another directory (using `--root` flag)
```bash
flakes-drift-checker --update --root /etc/nixos/
```
### Running history records (could be very useful for you to track your updates)
```bash
flakes-drift-checker history
```

# Dev env running:

### running help
```bash
nix-shell ./shell.nix --run './gradlew :app:run --args="--help"'
```
### running help for special sections (`check`, for example)
```bash
nix-shell ./shell.nix --run './gradlew :app:run --args="check --help"'
```
### point to your flake root:
```bash
nix-shell ./shell.nix --run './gradlew :app:run --rerun --args="check --root /etc/nixos/"'
```
### running actual updater
```bash
nix-shell ./shell.nix --run './gradlew :app:run --rerun --args="update --root /etc/nixos"'
```
### history 
```bash
nix-shell ./shell.nix --run './gradlew :app:run --args="history"'
```
### Tests 
```bash
nix-shell --command "./gradlew test"
```

### Build (Native image)
```bash
nix-shell shell.nix --run "GRAALVM_HOME=$(ls -d /nix/store/*-graalvm-ce-* ) ./gradlew :app:nativeCompile"
```

### Run (Native image) 
```bash
app/build/native/nativeCompile/flakes-drift-checker --help
```

# Dataset manipulations (In case you ever need it)

### 1. Mirror + regenerate the generalized fixture tree (flake.nix + flake.lock)
```bash
drift-trace/src/test/resources/scripts/gen_fixture.sh \
     --flake-root-path $(pwd)/drift-trace/src/test/resources/flakes/staging-machine \
     --out-path /tmp/out-fixture"
```
### 2. Generate the mock /nix/store INTO the freshly generated fixture
```bash
drift-trace/src/test/resources/scripts/gen_mock_store.sh \
     --path /tmp/out-fixture \
     --out-path /tmp/out-fixture/nix/store
```
### 3. Regenerate the dataset JSON (works on any fixture root)
```bash
drift-trace/src/test/resources/scripts/gen_dataset.sh \
     --path $(pwd)/drift-trace/src/test/resources/flakes/staging-machine \
     --out-path $(pwd)/drift-trace/src/test/resources/datasets/staging-machine.json
```

# Some notes I would like to leave here as a reminder of untypical project structure: 
This one built with Java and compiled via GraalVM Native Image (and it runs as fast as your rust application too! Java is not slow).
1. graalVm config is at `/app/src/main/resources/META-INF/native-image/com.nix.flakedrift/nix-drift-app/reflect-config.json`
2. repository declarations are centralized in project root (e.g. settings.gradle.kts) for nix's buildGradleApplication
3. added verification-metadata.xml as a "dependency lock" for a nix build where  `mkM2Repository` builds `depSpecs` through `runCommand`
4. gradle builds from offline m2 via `--offline` thanks to point 3. (MAVEN_SOURCE_REPOSITORY).
5. buildGradleApplication also requires distributionSha256Sum in `./gradle/wrapper/gradle-wrapper.properties` inside the sandboxed nix build.
6. `app/src/main/resources/META-INF/native-image/com.nix.flakedrift/nix-drift-app/resource-config.json` tells GraalVM to include `version.properties` into binary
7. added `gradle.properties` in root to have group and versions unified source. 
8. running `nix develop --command ./gradlew verificationSync` syncs (e.g., updates, not re-generates) `verification-metadata.xml` if new dependencies introduced (no need in manual bump).
9. `verification-metadata.xml`  `<configuration>` tag is manual-written, `<components>` is automated via step 8. 