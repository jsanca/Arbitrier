package com.arbitrier.platform;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition;
import org.junit.jupiter.api.Test;

class PlatformArchitectureTest {

    private static final String BASE_PACKAGE = "com.arbitrier.platform";

    private final JavaClasses classes = new ClassFileImporter().importPackages(BASE_PACKAGE);

    @Test
    void platform_must_not_reference_business_domain_packages() {
        ArchRuleDefinition.noClasses().that().resideInAPackage("com.arbitrier.platform..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "com.arbitrier.order..",
                        "com.arbitrier.inventory..",
                        "com.arbitrier.credit..",
                        "com.arbitrier.orchestrator..")
                .allowEmptyShould(true)
                .check(classes);
    }

    @Test
    void messaging_domain_must_not_depend_on_jpa_or_spring() {
        ArchRuleDefinition.noClasses().that()
                .resideInAnyPackage(
                        "com.arbitrier.platform.messaging.outbox",
                        "com.arbitrier.platform.messaging.inbox",
                        "com.arbitrier.platform.messaging.serialization",
                        "com.arbitrier.platform.messaging.outbox.mapper")
                .should().dependOnClassesThat()
                .resideInAnyPackage(
                        "jakarta.persistence..",
                        "org.springframework..",
                        "org.hibernate..")
                .allowEmptyShould(true)
                .check(classes);
    }

    @Test
    void messaging_adapter_must_not_depend_on_domain() {
        ArchRuleDefinition.noClasses().that()
                .resideInAnyPackage(
                        "com.arbitrier.platform.messaging.outbox.adapter..",
                        "com.arbitrier.platform.messaging.inbox.adapter..")
                .should().dependOnClassesThat()
                .resideInAnyPackage(
                        "com.arbitrier.order..",
                        "com.arbitrier.inventory..",
                        "com.arbitrier.credit..",
                        "com.arbitrier.orchestrator..")
                .allowEmptyShould(true)
                .check(classes);
    }

    @Test
    void objectMapper_confined_to_jackson_infrastructure() {
        ArchRuleDefinition.noClasses().that()
                .resideOutsideOfPackage("..jackson..")
                .and().resideOutsideOfPackage("..serialization..")
                .and().resideOutsideOfPackage("..spring..")
                .should().dependOnClassesThat()
                .resideInAnyPackage("com.fasterxml.jackson.databind..")
                .allowEmptyShould(true)
                .check(classes);
    }

    @Test
    void messaging_public_api_exposes_only_eventSerializer() {
        ArchRuleDefinition.classes().that()
                .resideInAnyPackage(
                        "com.arbitrier.platform.messaging.outbox",
                        "com.arbitrier.platform.messaging.inbox")
                .should().dependOnClassesThat()
                .resideInAnyPackage("com.fasterxml.jackson..")
                .allowEmptyShould(true)
                .check(classes);
    }
}
