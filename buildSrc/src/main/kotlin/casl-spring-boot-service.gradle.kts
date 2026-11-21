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
	testCompileOnly("org.projectlombok:lombok")
	testAnnotationProcessor("org.projectlombok:lombok")
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
