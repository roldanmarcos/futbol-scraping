package com.futbol.scraping.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RestController;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

class LayeredArchitectureTest {

        private static final String BASE_PACKAGE = "com.futbol.scraping";
        private static final String CONTROLLERS = "Controllers";
        private static final String SERVICES = "Services";
        private static final String REPOSITORIES = "Repositories";
        private static final String ADAPTERS = "Adapters";

        private final JavaClasses classes = new ClassFileImporter()
                        .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                        .importPackages(BASE_PACKAGE);

        @Test
        void layeredArchitectureShouldBeRespected() {
                layeredArchitecture()
                                .consideringOnlyDependenciesInLayers()

                                .layer(CONTROLLERS).definedBy(BASE_PACKAGE + ".web..")
                                .layer(SERVICES).definedBy(BASE_PACKAGE + ".service..")
                                .layer(REPOSITORIES).definedBy(BASE_PACKAGE + ".repository..")
                                .layer(ADAPTERS).definedBy(BASE_PACKAGE + ".adapter..")

                                .whereLayer(CONTROLLERS).mayOnlyAccessLayers(SERVICES)
                                .whereLayer(SERVICES).mayOnlyAccessLayers(REPOSITORIES, ADAPTERS)
                                .whereLayer(REPOSITORIES).mayOnlyBeAccessedByLayers(SERVICES)
                                .whereLayer(ADAPTERS).mayOnlyBeAccessedByLayers(SERVICES)

                                .check(classes);
        }

        @Test
        void controllersShouldNotDependOnRepositoriesOrAdapters() {
                noClasses()
                                .that().resideInAnyPackage(BASE_PACKAGE + ".web..")
                                .should().dependOnClassesThat().resideInAnyPackage(
                                                BASE_PACKAGE + ".repository..",
                                                BASE_PACKAGE + ".adapter..")
                                .because("Controllers solo pueden comunicarse con Services")
                                .check(classes);
        }

        @Test
        void servicesShouldBeTheOnlyLayerAccessingRepositoriesAndAdapters() {
                noClasses()
                                .that().resideOutsideOfPackage(BASE_PACKAGE + ".service..")
                                .should().dependOnClassesThat().resideInAnyPackage(
                                                BASE_PACKAGE + ".repository..",
                                                BASE_PACKAGE + ".adapter..")
                                .because("Solo Services puede acceder a Repositories y Adapters")
                                .check(classes);
        }

        @Test
        void lowerLayersShouldNotDependOnUpperLayers() {
                noClasses()
                                .that().resideInAnyPackage(
                                                BASE_PACKAGE + ".repository..",
                                                BASE_PACKAGE + ".adapter..")
                                .should().dependOnClassesThat().resideInAnyPackage(
                                                BASE_PACKAGE + ".service..",
                                                BASE_PACKAGE + ".web..")
                                .because("Ninguna capa inferior puede depender de capas superiores")
                                .check(classes);
        }

        @Test
        void controllersShouldHaveSuffixController() {
                classes()
                                .that().resideInAPackage(BASE_PACKAGE + ".web..")
                                .should().haveSimpleNameEndingWith("Controller")
                                .because("Todos los controllers deben terminar con el sufijo Controller")
                                .check(classes);
        }

        @Test
        void servicesShouldHaveSuffixService() {
                classes()
                                .that().resideInAPackage(BASE_PACKAGE + ".service..")
                                .and().areNotEnums()
                                .and().areNotAnonymousClasses()
                                .should().haveSimpleNameEndingWith("Service")
                                .because("Todos los services deben terminar con el sufijo Service")
                                .check(classes);
        }

        @Test
        void repositoriesShouldHaveSuffixRepository() {
                classes()
                                .that().resideInAPackage(BASE_PACKAGE + ".repository..")
                                .and().areInterfaces()
                                .should().haveSimpleNameEndingWith("Repository")
                                .because("Todas las interfaces en repositories deben terminar con el sufijo Repository")
                                .check(classes);
        }

        @Test
        void controllersShouldBeAnnotatedWithRestController() {
                classes()
                                .that().resideInAPackage(BASE_PACKAGE + ".web..")
                                .should().beAnnotatedWith(RestController.class)
                                .because("Todos los controllers deben estar anotados con @RestController")
                                .check(classes);
        }

        @Test
        void servicesShouldBeAnnotatedWithService() {
                classes()
                                .that().resideInAPackage(BASE_PACKAGE + ".service..")
                                .and().areNotEnums()
                                .and().areNotAnonymousClasses()
                                .should().beAnnotatedWith(Service.class)
                                .because("Todos los services deben estar anotados con @Service")
                                .check(classes);
        }
}
