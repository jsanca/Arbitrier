package com.arbitrier.order.unit;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.jupiter.api.Test;

class ArchitectureTest {

    private static final String BASE_PACKAGE = "com.arbitrier.order";

    private final JavaClasses classes = new ClassFileImporter().importPackages(BASE_PACKAGE);

    @Test
    void domain_must_not_depend_on_adapter() {
        // Activate with ArchUnit rule once domain classes exist:
        // noClasses().that().resideInAPackage("..domain..")
        //     .should().dependOnClassesThat().resideInAPackage("..adapter..")
        //     .check(classes);
    }

    @Test
    void application_must_not_depend_on_adapter() {
        // Activate with ArchUnit rule once application classes exist:
        // noClasses().that().resideInAPackage("..application..")
        //     .should().dependOnClassesThat().resideInAPackage("..adapter..")
        //     .check(classes);
    }

    @Test
    void domain_must_not_import_spring_or_jpa() {
        // Activate once domain classes exist:
        // noClasses().that().resideInAPackage("..domain..")
        //     .should().dependOnClassesThat().resideInAnyPackage("org.springframework..", "jakarta.persistence..")
        //     .check(classes);
    }
}
