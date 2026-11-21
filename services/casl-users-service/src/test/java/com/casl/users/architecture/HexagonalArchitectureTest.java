package com.casl.users.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Verifica automáticamente las reglas de dependencia de ADR-003.
 */
@AnalyzeClasses(packages = "com.casl.users", importOptions = ImportOption.DoNotIncludeTests.class)
class HexagonalArchitectureTest {

	@ArchTest
	static final ArchRule domain_is_framework_free = classes()
			.that().resideInAPackage("..domain..")
			.should().onlyDependOnClassesThat()
			.resideInAnyPackage("java..", "com.casl.users.domain..");

	@ArchTest
	static final ArchRule domain_does_not_know_outer_layers = noClasses()
			.that().resideInAPackage("..domain..")
			.should().dependOnClassesThat()
			.resideInAnyPackage("..application..", "..adapter..", "..config..");

	@ArchTest
	static final ArchRule application_does_not_know_adapters = noClasses()
			.that().resideInAPackage("..application..")
			.should().dependOnClassesThat()
			.resideInAnyPackage("..adapter..", "..config..");

	@ArchTest
	static final ArchRule inbound_does_not_know_outbound = noClasses()
			.that().resideInAPackage("..adapter.in..")
			.should().dependOnClassesThat()
			.resideInAPackage("..adapter.out..");

	@ArchTest
	static final ArchRule outbound_does_not_know_inbound = noClasses()
			.that().resideInAPackage("..adapter.out..")
			.should().dependOnClassesThat()
			.resideInAPackage("..adapter.in..");
}
