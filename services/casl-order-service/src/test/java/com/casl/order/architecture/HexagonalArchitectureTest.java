package com.casl.order.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(packages = "com.casl.order", importOptions = ImportOption.DoNotIncludeTests.class)
class HexagonalArchitectureTest {

	@ArchTest
	static final ArchRule domain_is_framework_free = classes()
			.that().resideInAPackage("..domain..")
			.should().onlyDependOnClassesThat()
			.resideInAnyPackage("java..", "com.casl.order.domain..");

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
}
