plugins {
    id("java-library")
    id("maven-publish")
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

dependencies {
    implementation("com.fasterxml.jackson.core:jackson-databind:2.18.2")

    testImplementation(platform("org.junit:junit-bom:6.0.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("org.mockito:mockito-core:5.14.2")
    testImplementation("org.mockito:mockito-junit-jupiter:5.14.2")
}

// NIX-BUILD / VERSION NOTE: the version lives ONLY in the root gradle.properties
// (set via `group=`/`version=`). It flows into the jar here; for the runtime we
// also stamp it into a version.properties resource so Java (AppConfiguration)
// and the native image can read the same value without any hardcoded duplicate.
val generatedVersionDir = layout.buildDirectory.dir("generated/version")

val generateVersionResource by tasks.registering {
    outputs.dir(generatedVersionDir)
    doLast {
        val file = generatedVersionDir.get().file("version.properties").asFile
        file.parentFile.mkdirs()
        file.writeText("version=" + project.version + "\n")
    }
}

sourceSets.main.get().resources.srcDir(generatedVersionDir)

tasks.named("processResources") {
    dependsOn(generateVersionResource)
}

tasks.test {
    useJUnitPlatform()
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }
    }
}