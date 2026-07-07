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
                .check(classes);
    }
}
