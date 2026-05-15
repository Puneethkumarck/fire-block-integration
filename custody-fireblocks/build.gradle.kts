plugins {
    kotlin("jvm")
    kotlin("plugin.spring")
    kotlin("plugin.jpa")
    id("org.springframework.boot")
    id("io.spring.dependency-management")
    `java-test-fixtures`
    jacoco
}

kotlin {
    jvmToolchain(25)
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict")
    }
}

sourceSets {
    create("integrationTest") {
        compileClasspath += sourceSets.main.get().output + sourceSets["testFixtures"].output
        runtimeClasspath += sourceSets.main.get().output + sourceSets["testFixtures"].output
    }
    create("businessTest") {
        compileClasspath += sourceSets.main.get().output + sourceSets["testFixtures"].output
        runtimeClasspath += sourceSets.main.get().output + sourceSets["testFixtures"].output
    }
}

val integrationTestImplementation by configurations.getting {
    extendsFrom(configurations.testImplementation.get())
}
val integrationTestRuntimeOnly by configurations.getting {
    extendsFrom(configurations.testRuntimeOnly.get())
}
val businessTestImplementation by configurations.getting {
    extendsFrom(configurations.testImplementation.get())
}
val businessTestRuntimeOnly by configurations.getting {
    extendsFrom(configurations.testRuntimeOnly.get())
}

dependencies {
    implementation(project(":custody-fireblocks-api"))
    implementation(platform("org.testcontainers:testcontainers-bom:1.21.4"))

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.kafka:spring-kafka")
    implementation("org.springframework.boot:spring-boot-starter-flyway")
    implementation("org.flywaydb:flyway-database-postgresql")
    implementation("org.postgresql:postgresql")
    implementation("io.github.resilience4j:resilience4j-spring-boot4:2.4.0")
    implementation(platform("software.amazon.awssdk:bom:2.31.62"))
    implementation("software.amazon.awssdk:secretsmanager")
    implementation("io.jsonwebtoken:jjwt-api:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.13.0")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.6")

    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(group = "org.mockito")
    }
    testImplementation("io.mockk:mockk:1.14.9")
    testImplementation("com.ninja-squad:springmockk:4.0.2")
    testImplementation("com.tngtech.archunit:archunit-junit5:1.4.2")

    testFixturesImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(group = "org.mockito")
    }

    integrationTestImplementation("org.testcontainers:junit-jupiter")
    integrationTestImplementation("org.testcontainers:postgresql")
    integrationTestImplementation("org.testcontainers:kafka")
    integrationTestImplementation("org.testcontainers:localstack")
    integrationTestImplementation("org.springframework.boot:spring-boot-testcontainers")
    integrationTestImplementation("org.springframework.security:spring-security-test")
    integrationTestImplementation("org.wiremock:wiremock-standalone:3.12.1")
    integrationTestImplementation(testFixtures(project))

    businessTestImplementation(project(":custody-fireblocks-client"))
    businessTestImplementation("org.testcontainers:junit-jupiter")
    businessTestImplementation("org.testcontainers:postgresql")
    businessTestImplementation("org.testcontainers:kafka")
    businessTestImplementation("org.testcontainers:localstack")
    businessTestImplementation("org.springframework.boot:spring-boot-testcontainers")
    businessTestImplementation("org.springframework.security:spring-security-test")
    businessTestImplementation("org.wiremock:wiremock-standalone:3.12.1")
    businessTestImplementation(testFixtures(project))

    developmentOnly("org.springframework.boot:spring-boot-devtools")
}

val integrationTest by tasks.registering(Test::class) {
    testClassesDirs = sourceSets["integrationTest"].output.classesDirs
    classpath = sourceSets["integrationTest"].runtimeClasspath
    useJUnitPlatform()
}

val businessTest by tasks.registering(Test::class) {
    testClassesDirs = sourceSets["businessTest"].output.classesDirs
    classpath = sourceSets["businessTest"].runtimeClasspath
    useJUnitPlatform()
}

tasks.named("check") {
    dependsOn(integrationTest, businessTest)
}

jacoco {
    toolVersion = "0.8.14"
}

tasks.jacocoTestReport {
    dependsOn(tasks.test, integrationTest, businessTest)

    executionData.setFrom(
        fileTree(layout.buildDirectory) {
            include("jacoco/*.exec")
        },
    )

    reports {
        xml.required.set(true)
        html.required.set(true)
        csv.required.set(false)
    }

    classDirectories.setFrom(
        files(
            classDirectories.files.map {
                fileTree(it) {
                    exclude(
                        "**/CustodyFireblocksApplication*",
                        "**/Q*",
                        "**/*MapperImpl*",
                        "**/*\$*",
                    )
                }
            },
        ),
    )
}

tasks.jacocoTestCoverageVerification {
    dependsOn(tasks.jacocoTestReport)

    executionData.setFrom(
        fileTree(layout.buildDirectory) {
            include("jacoco/*.exec")
        },
    )

    classDirectories.setFrom(tasks.jacocoTestReport.get().classDirectories)

    violationRules {
        rule {
            limit {
                counter = "LINE"
                minimum = "0.80".toBigDecimal()
            }
            limit {
                counter = "BRANCH"
                minimum = "0.70".toBigDecimal()
            }
        }
    }
}
