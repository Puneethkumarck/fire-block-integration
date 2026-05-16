package com.stablecoin.custody.fireblocks

import com.tngtech.archunit.core.domain.JavaClass
import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.core.importer.ImportOption
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noFields
import com.tngtech.archunit.library.Architectures.layeredArchitecture
import com.tngtech.archunit.library.GeneralCodingRules.NO_CLASSES_SHOULD_ACCESS_STANDARD_STREAMS
import com.tngtech.archunit.library.GeneralCodingRules.NO_CLASSES_SHOULD_THROW_GENERIC_EXCEPTIONS
import com.tngtech.archunit.library.GeneralCodingRules.NO_CLASSES_SHOULD_USE_JAVA_UTIL_LOGGING
import org.junit.jupiter.api.Test

class ArchitectureTest {
    private val basePackage = "com.stablecoin.custody.fireblocks"

    private val classes =
        ClassFileImporter()
            .withImportOption(ImportOption.DoNotIncludeTests())
            .importPackages(basePackage)

    @Test
    fun `should enforce hexagonal layer dependencies`() {
        layeredArchitecture()
            .consideringOnlyDependenciesInLayers()
            .withOptionalLayers(true)
            .layer("APPLICATION")
            .definedBy("$basePackage.application..")
            .layer("DOMAIN")
            .definedBy("$basePackage.domain..")
            .layer("INFRASTRUCTURE")
            .definedBy("$basePackage.infrastructure..")
            .whereLayer("APPLICATION")
            .mayOnlyAccessLayers("DOMAIN", "INFRASTRUCTURE")
            .whereLayer("DOMAIN")
            .mayNotAccessAnyLayer()
            .whereLayer("INFRASTRUCTURE")
            .mayOnlyAccessLayers("DOMAIN")
            .check(classes)
    }

    @Test
    fun `domain must not import Spring except stereotype and transaction`() {
        noClasses()
            .that()
            .resideInAPackage("$basePackage.domain..")
            .should()
            .dependOnClassesThat(
                JavaClass.Predicates
                    .resideInAPackage("org.springframework..")
                    .and(JavaClass.Predicates.resideOutsideOfPackage("org.springframework.stereotype.."))
                    .and(JavaClass.Predicates.resideOutsideOfPackage("org.springframework.transaction..")),
            ).check(classes)
    }

    @Test
    fun `domain must not import JPA`() {
        noClasses()
            .that()
            .resideInAPackage("$basePackage.domain..")
            .should()
            .dependOnClassesThat()
            .resideInAPackage("jakarta.persistence..")
            .check(classes)
    }

    @Test
    fun `no classes should access standard streams`() {
        NO_CLASSES_SHOULD_ACCESS_STANDARD_STREAMS.check(classes)
    }

    @Test
    fun `no classes should use java util logging`() {
        NO_CLASSES_SHOULD_USE_JAVA_UTIL_LOGGING.check(classes)
    }

    @Test
    fun `no classes should throw generic exceptions`() {
        NO_CLASSES_SHOULD_THROW_GENERIC_EXCEPTIONS.check(classes)
    }

    @Test
    fun `no fields should be annotated with Autowired`() {
        noFields()
            .should()
            .beAnnotatedWith("org.springframework.beans.factory.annotation.Autowired")
            .check(classes)
    }
}
