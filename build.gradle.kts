import javax.inject.Inject
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations

plugins {
    id("java")
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:6.0.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}

// --- Verification-metadata maintenance (run from `nix develop`, needs JDK 21) ---
//
// Gradle only writes gradle/verification-metadata.xml through the
// `--write-verification-metadata` CLI flag (there is no task API for it), so
// the task below drives that flow: it re-resolves the real build PLUS the
// `-sources.jar` files the IDE pulls during sync, and re-records checksums.
// The result is identical to running:
//   ./gradlew --write-verification-metadata sha256 build resolveVerificationSources
//
// IDE-only files that no build task ever resolves (Gradle-distro kotlin
// .module/.pom, gradle-*-src.zip) stay in verification-metadata.xml as
// recorded trust/checksum entries; reload reads them, it does not re-generate.
// Keep the lockfile in sync after dependency bumps with:
//   nix develop --command ./gradlew verificationSync

tasks.register("resolveVerificationSources") {
    group = "verification"
    description = "Download the -sources.jar of every test-runtime dependency (IDE-only artifacts)."
    doLast {
        val scratch = configurations.create("verificationSourcesScratch") {
            isTransitive = false
        }
        val existing = configurations.getByName("testRuntimeClasspath")
        existing.resolvedConfiguration.resolvedArtifacts.forEach { a ->
            val v = a.moduleVersion.id
            scratch.dependencies.add(dependencies.create("${v.group}:${v.name}:${v.version}:sources"))
        }
        try {
            println("Resolved ${scratch.resolve().size} source jars")
        } catch (e: Exception) {
            println("Warning: some source jars unavailable: ${e.message?.take(200)}")
        }
    }
}

abstract class VerificationSyncTask : DefaultTask() {
    @get:Inject
    abstract val execOperations: ExecOperations

    @TaskAction
    fun sync() {
        val wrapper = project.rootDir.resolve("gradlew").absolutePath
        execOperations.exec {
            workingDir = project.rootDir
            commandLine = listOf(
                wrapper,
                "--write-verification-metadata", "sha256",
                "build", "resolveVerificationSources",
                "--console=plain",
            )
        }
    }
}

tasks.register<VerificationSyncTask>("verificationSync") {
    group = "verification"
    description = "Regenerate gradle/verification-metadata.xml (build graph + -sources.jar) without hand-editing."
    dependsOn("resolveVerificationSources")
}