// Restricciones de versión por vulnerabilidades (Fase 9, security hardening).
// NOTA: la versión de CycloneDX queda fijada en un solo lugar — el catálogo
// (`gradle/libs.versions.toml`). Si Gradle 9.9.2 permite alguna vez apply
// con versión via alias, sincronizar con el catálogo antes de hacerlo.
import org.cyclonedx.gradle.CyclonedxDirectTask
import org.cyclonedx.model.Component
import org.gradle.jvm.toolchain.JavaLanguageVersion

plugins {
	java
	id("org.springframework.boot")
	id("io.spring.dependency-management")
	jacoco
}

repositories {
	mavenCentral()
}

// Toolchain Java 21 forzada desde la convención para todos los servicios.
java {
	toolchain {
		languageVersion.set(JavaLanguageVersion.of(21))
	}
}

configurations.configureEach {
	// Exclusión global de spring-boot-starter-logging: se mantiene Log4j2
	// como única implementación de logging (ver ADR-002).
	exclude(group = "org.springframework.boot", module = "spring-boot-starter-logging")
}

dependencies {
	compileOnly("org.projectlombok:lombok")
	annotationProcessor("org.projectlombok:lombok")
	// Genera Log4j2Plugins.dat para registrar plugins propios (@Plugin) sin
	// depender del escaneo de paquetes, que está deprecado.
	annotationProcessor("org.apache.logging.log4j:log4j-core")
	// Layout JSON para logs estructurados con event template propio
	// (ver ADR-016 / casl-log-event*.json).
	runtimeOnly("org.apache.logging.log4j:log4j-layout-template-json")
	testCompileOnly("org.projectlombok:lombok")
	testAnnotationProcessor("org.projectlombok:lombok")
}

// ---------------------------------------------------------------------------
// Restricciones de versión por vulnerabilidades (Fase 9, security hardening).
// Estas dos librerías son dependencias transitivas del cliente Kafka y solo
// aparecen en los módulos que arrastran kafka-clients: constraints, no
// dependencias directas, para no introducir la librería donde no existe.
// Revisar/eliminar este bloque cuando el BOM de Spring Boot (o kafka-clients)
// publique una versión que ya incluya las versiones corregidas, o si Trivy
// vuelve a reportar hallazgos sobre estas librerías.
// ---------------------------------------------------------------------------
dependencies {
	constraints {
		// CVE-2026-106451
		implementation("at.yawk.lz4:lz4-java:1.11.4") {
			because("CVE-2026-106451")
		}
		// CVE-2026-93451 / CVE-2026-108106
		implementation("org.xerial.snappy:snappy-java:1.1.10.9") {
			because("CVE-2026-93451 / CVE-2026-108106")
		}
	}
}

// ---------------------------------------------------------------------------
// Tests de integración en src/integrationTest/java (Testcontainers).
// Separados de los unitarios para que el pipeline pueda exigir ambos en verde.
// ---------------------------------------------------------------------------
val integrationTestSourceSet = sourceSets.create("integrationTest") {
	compileClasspath += sourceSets.getByName("main").output
	runtimeClasspath += sourceSets.getByName("main").output
}

configurations[integrationTestSourceSet.implementationConfigurationName]
	.extendsFrom(configurations.getByName("testImplementation"))
configurations[integrationTestSourceSet.runtimeOnlyConfigurationName]
	.extendsFrom(configurations.getByName("testRuntimeOnly"))
configurations[integrationTestSourceSet.annotationProcessorConfigurationName]
	.extendsFrom(configurations.getByName("testAnnotationProcessor"))

val integrationTest = tasks.register<Test>("integrationTest") {
	description = "Ejecuta los tests de integración (Testcontainers)."
	group = "verification"
	testClassesDirs = integrationTestSourceSet.output.classesDirs
	classpath = integrationTestSourceSet.runtimeClasspath
	shouldRunAfter(tasks.named("test"))
}

tasks.withType<Test> {
	useJUnitPlatform()
	finalizedBy(tasks.named("jacocoTestReport"))
}

// La cobertura suma unitarios + integración.
tasks.jacocoTestReport {
	executionData(tasks.named("test").get(), integrationTest.get())
	reports {
		xml.required.set(true)
		html.required.set(true)
	}
}

tasks.jacocoTestCoverageVerification {
	executionData(tasks.named("test").get(), integrationTest.get())
	mustRunAfter(tasks.named("test"), integrationTest)
	violationRules {
		rule {
			limit {
				counter = "LINE"
				value = "COVEREDRATIO"
				minimum = "0.50".toBigDecimal()
			}
		}
	}
}

tasks.check {
	dependsOn(integrationTest, tasks.named("jacocoTestCoverageVerification"))
}

// ---------------------------------------------------------------------------
// SBOM CycloneDX (Fase 9): un JSON por módulo en build/reports/sbom/<>-bom.json.
// El plugin se aplica una sola vez en la raíz; aquí solo se configura la tarea
// directa de cada módulo. La API es la de org.cyclonedx.bom 3.6.1 compilada:
// jsonOutput/xmlOutput son RegularFileProperty (no booleanos) y no existe
// outputName/destination: la ruta se fija en jsonOutput.
// ---------------------------------------------------------------------------
tasks.named<CyclonedxDirectTask>("cyclonedxDirectBom") {
	projectType.set(Component.Type.APPLICATION)
	includeConfigs.set(listOf("runtimeClasspath", "compileClasspath"))
	failOnUnresolvedDependencies.set(true)
	jsonOutput.set(layout.buildDirectory.file("reports/sbom/${project.name}-bom.json"))
	// Solo JSON: se elimina la convención que escribiría el XML por defecto.
	xmlOutput.unsetConvention()
}
