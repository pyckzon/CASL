import org.springframework.boot.gradle.tasks.bundling.BootBuildImage

plugins {
	id("casl-spring-boot-service")
}

group = "com.casl"
version = "0.0.1-SNAPSHOT"

dependencies {
	implementation(libs.spring.boot.starter.webmvc)
	implementation(libs.spring.boot.starter.log4j2)
	implementation(libs.spring.boot.starter.actuator)
	implementation(libs.spring.boot.starter.data.jpa)
	implementation(libs.spring.boot.starter.validation)
	implementation(libs.spring.boot.flyway)
	implementation(libs.flyway.core)
	runtimeOnly(libs.flyway.database.postgresql)
	runtimeOnly(libs.postgresql)
	developmentOnly(libs.spring.boot.devtools)

	testImplementation(libs.spring.boot.starter.test)
	testImplementation(libs.spring.boot.starter.webmvc.test)
	testImplementation(libs.archunit.junit5)

	// Testcontainers solo en el source set de integración: los tests unitarios
	// no necesitan Docker y deben seguir siendo rápidos.
	integrationTestImplementation(libs.spring.boot.testcontainers)
	integrationTestImplementation(libs.testcontainers.postgresql)
	integrationTestImplementation(libs.testcontainers.junit.jupiter)
}

tasks.named<BootBuildImage>("bootBuildImage") {
	imageName.set("casl/casl-users-service:0.0.1-SNAPSHOT")
}
