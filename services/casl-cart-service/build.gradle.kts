import org.springframework.boot.gradle.tasks.bundling.BootBuildImage

plugins {
	id("casl-spring-boot-service")
}

group = "com.casl"
version = "0.0.1-SNAPSHOT"

dependencies {
	implementation(libs.spring.boot.starter.webmvc)
	implementation(libs.spring.boot.starter.restclient)
	implementation(libs.spring.boot.starter.log4j2)
	implementation(libs.spring.boot.starter.actuator)
	implementation(libs.spring.boot.starter.validation)
	implementation(libs.spring.boot.starter.data.redis)
	implementation(libs.spring.boot.starter.oauth2.resource.server)
	implementation(project(":services:casl-security"))
	developmentOnly(libs.spring.boot.devtools)

	testImplementation(libs.spring.boot.starter.test)
	testImplementation(libs.spring.boot.restclient.test)
	testImplementation(libs.spring.boot.starter.webmvc.test)
	testImplementation(libs.spring.security.test)
	testImplementation(libs.spring.boot.security.test)
	testImplementation(libs.archunit.junit5)
	testImplementation(libs.testcontainers.junit.jupiter)

	integrationTestImplementation(libs.spring.boot.testcontainers)
	integrationTestImplementation(libs.testcontainers.junit.jupiter)
}

tasks.named<BootBuildImage>("bootBuildImage") {
	imageName.set("casl/casl-cart-service:0.0.1-SNAPSHOT")
}

tasks.named("jacocoTestCoverageVerification") {
	enabled = false
}
