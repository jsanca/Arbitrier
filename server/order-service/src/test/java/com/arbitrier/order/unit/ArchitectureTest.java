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

    /**
     * Publication boundary rule (ARB-024.1):
     * Application services must write domain events to the transactional Outbox only.
     * KafkaTemplate and Kafka producer adapters belong exclusively to the transport/relay layer.
     * No application class may hold a reference to a Kafka publisher of any kind.
     */
    @Test
    void application_must_not_depend_on_kafka_publisher_adapters() {
        ArchRuleDefinition.noClasses().that().resideInAPackage("..application..")
                .should().dependOnClassesThat().resideInAPackage("..adapter.outbound.kafka..")
                .because("application services publish through OutboxRepository; " +
                         "transport publication adapters belong in the adapter layer")
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
    void domain_must_not_depend_on_grpc_or_protobuf() {
        ArchRuleDefinition.noClasses().that().resideInAPackage("..domain..")
                .should().dependOnClassesThat()
                .resideInAnyPackage("io.grpc..", "com.google.protobuf..", "com.arbitrier.contracts..")
                .allowEmptyShould(true)
                .check(classes);
    }

    @Test
    void application_must_not_depend_on_grpc_or_protobuf() {
        ArchRuleDefinition.noClasses().that().resideInAPackage("..application..")
                .should().dependOnClassesThat()
                .resideInAnyPackage("io.grpc..", "com.google.protobuf..", "com.arbitrier.contracts..")
                .allowEmptyShould(true)
                .check(classes);
    }

    @Test
    void grpc_adapter_must_reside_in_adapter_outbound_grpc_package() {
        ArchRuleDefinition.noClasses().that()
                .resideInAPackage("..adapter..")
                .and().resideOutsideOfPackage("..adapter.outbound.grpc..")
                .should().dependOnClassesThat().resideInAPackage("io.grpc..")
                .allowEmptyShould(true)
                .check(classes);
    }
}
