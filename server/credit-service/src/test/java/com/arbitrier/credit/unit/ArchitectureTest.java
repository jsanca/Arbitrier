package com.arbitrier.credit.unit;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.jupiter.api.Test;

class ArchitectureTest {

    private static final String BASE_PACKAGE = "com.arbitrier.credit";

    private final JavaClasses classes = new ClassFileImporter().importPackages(BASE_PACKAGE);

    @Test
    void domain_must_not_depend_on_adapter() {
        // Activate once domain classes exist:
        // noClasses().that().resideInAPackage("..domain..")
        //     .should().dependOnClassesThat().resideInAPackage("..adapter..")
        //     .check(classes);
    }

    @Test
    void application_must_not_depend_on_adapter() {
        // Activate once application classes exist:
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
