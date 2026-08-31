plugins {
    id("application")
    id("org.graalvm.buildtools.native") version "0.10.6"
}

dependencies {
    implementation(project(":drift-trace"))
    implementation("info.picocli:picocli:4.7.6")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.18.2")

    testImplementation(platform("org.junit:junit-bom:6.0.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

application {
    mainClass.set("com.nix.flakedrift.app.Main")
}

base {
    archivesName.set("nix-drift-app")
}

tasks.test {
    useJUnitPlatform()
}

graalvmNative {
    // NIX-BUILD NOTE: the reachability-metadata repository would trigger a
    // network download during native-image — it is disabled so the sandboxed
    // `nix build` (flake.nix, offline gradle) can succeed. Local builds keep
    // working fine without it.
    metadataRepository {
        enabled = false
    }
    binaries {
        named("main") {
            imageName.set("flakes-drift-checker")
            mainClass.set("com.nix.flakedrift.app.Main")
            buildArgs.add("--no-fallback")
            buildArgs.add("-H:+ReportExceptionStackTraces")
        }
    }
}
