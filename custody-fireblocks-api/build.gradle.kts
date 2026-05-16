plugins {
    kotlin("jvm")
    `java-library`
    `java-test-fixtures`
    id("io.spring.dependency-management")
    id("com.github.davidmc24.gradle.plugin.avro") version "1.9.1"
}

dependencyManagement {
    imports {
        mavenBom(org.springframework.boot.gradle.plugin.SpringBootPlugin.BOM_COORDINATES)
    }
}

kotlin {
    jvmToolchain(25)
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict")
    }
}

repositories {
    maven("https://packages.confluent.io/maven/") {
        content {
            includeGroupByRegex("io\\.confluent(\\..+)?")
        }
    }
}

dependencies {
    api("jakarta.validation:jakarta.validation-api")
    api("com.fasterxml.jackson.core:jackson-annotations")
    api("org.apache.avro:avro:1.12.1")
}

tasks.matching { it.name == "compileTestFixturesKotlin" }.configureEach {
    dependsOn("generateTestFixturesAvroJava")
}

tasks.matching { it.name == "runKtlintCheckOverTestFixturesSourceSet" }.configureEach {
    dependsOn("generateTestFixturesAvroJava")
}

tasks.matching { it.name == "runKtlintFormatOverTestFixturesSourceSet" }.configureEach {
    dependsOn("generateTestFixturesAvroJava")
}

tasks.matching { it.name == "compileTestKotlin" }.configureEach {
    dependsOn("generateTestAvroJava")
}

tasks.matching { it.name == "runKtlintCheckOverTestSourceSet" }.configureEach {
    dependsOn("generateTestAvroJava")
}

tasks.matching { it.name == "runKtlintFormatOverTestSourceSet" }.configureEach {
    dependsOn("generateTestAvroJava")
}
