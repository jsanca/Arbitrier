package com.arbitrier.order.unit;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition;
import org.junit.jupiter.api.Test;

class ArchitectureTest {

    private static final String BASE_PACKAGE = "com.arbitrier.order";

    private final JavaClasses classes = new ClassFileImporter().importPackages(BASE_PACKAGE);

    @Test
    void domain_must_not_depend_on_adapter() {
        ArchRuleDefinition.noClasses().that().resideInAPackage("..domain..")
                .should().dependOnClassesThat().resideInAPackage("..adapter..")
                .allowEmptyShould(true)
                .check(classes);
    }

    @Test
    void application_must_not_depend_on_adapter() {
        ArchRuleDefinition.noClasses().that().resideInAPackage("..application..")
                .should().dependOnClassesThat().resideInAPackage("..adapter..")
                .allowEmptyShould(true)
                .check(classes);
    }

    @Test
    void domain_must_not_import_spring_or_jpa() {
        ArchRuleDefinition.noClasses().that().resideInAPackage("..domain..")
                .should().dependOnClassesThat().resideInAnyPackage("org.springframework..", "jakarta.persistence..")
                .allowEmptyShould(true)
                .check(classes);
    }

    @Test
    void domain_must_not_depend_on_avro_or_kafka() {
        ArchRuleDefinition.noClasses().that().resideInAPackage("..domain..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "org.apache.avro..", "org.springframework.kafka..", "org.apache.kafka..")
                .allowEmptyShould(true)
                .check(classes);
    }

    @Test
    void application_must_not_depend_on_avro_or_kafka() {
        ArchRuleDefinition.noClasses().that().resideInAPackage("..application..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "org.apache.avro..", "org.springframework.kafka..", "org.apache.kafka..")
                .allowEmptyShould(true)
                .check(classes);
    }
}
