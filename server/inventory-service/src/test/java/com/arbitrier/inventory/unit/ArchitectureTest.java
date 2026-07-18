package com.arbitrier.inventory.unit;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition;
import org.junit.jupiter.api.Test;

class ArchitectureTest {

    private static final String BASE_PACKAGE = "com.arbitrier.inventory";

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

    @Test
    void application_must_not_depend_on_spring_data() {
        ArchRuleDefinition.noClasses().that().resideInAPackage("..application..")
                .should().dependOnClassesThat()
                .resideInAPackage("org.springframework.data..")
                .allowEmptyShould(true)
                .check(classes);
    }

    @Test
    void domain_must_not_import_jpa_annotations() {
        ArchRuleDefinition.noClasses().that().resideInAPackage("..domain..")
                .should().dependOnClassesThat().resideInAnyPackage("jakarta.persistence..")
                .allowEmptyShould(true)
                .check(classes);
    }

    @Test
    void jpa_entities_must_reside_in_adapter_persistence_package() {
        ArchRuleDefinition.noClasses().that()
                .areAnnotatedWith("jakarta.persistence.Entity")
                .should().resideOutsideOfPackage("..adapter.outbound.persistence..")
                .allowEmptyShould(true)
                .check(classes);
    }

    @Test
    void application_must_not_depend_on_jpa() {
        ArchRuleDefinition.noClasses().that().resideInAPackage("..application..")
                .should().dependOnClassesThat().resideInAnyPackage("jakarta.persistence..")
                .allowEmptyShould(true)
                .check(classes);
    }

    @Test
    void grpc_adapter_must_not_depend_on_spring_data_repositories() {
        ArchRuleDefinition.noClasses().that().resideInAPackage("..adapter.inbound.grpc..")
                .should().dependOnClassesThat()
                .resideInAPackage("org.springframework.data..")
                .allowEmptyShould(true)
                .check(classes);
    }

    @Test
    void application_must_not_depend_on_protobuf() {
        ArchRuleDefinition.noClasses().that().resideInAPackage("..application..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "com.google.protobuf..", "com.arbitrier.contracts.inventory..")
                .allowEmptyShould(true)
                .check(classes);
    }

    @Test
    void domain_must_not_depend_on_grpc() {
        ArchRuleDefinition.noClasses().that().resideInAPackage("..domain..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "io.grpc..", "net.devh..", "com.google.protobuf..",
                        "com.arbitrier.contracts.inventory..")
                .allowEmptyShould(true)
                .check(classes);
    }
}
