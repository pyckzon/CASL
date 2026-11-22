import org.springframework.boot.gradle.tasks.bundling.BootJar

plugins {
	id("casl-spring-boot-service")
}

group = "com.casl"
version = "0.0.1-SNAPSHOT"

dependencies {
	implementation(libs.spring.boot.starter.webmvc)
	implementation(libs.spring.boot.starter.security)
	implementation(libs.spring.boot.starter.oauth2.resource.server)
	implementation(libs.spring.boot.starter.log4j2)

	testImplementation(libs.spring.boot.starter.test)
	testImplementation(libs.spring.boot.starter.webmvc.test)
	testImplementation(libs.spring.security.test)
	testImplementation(libs.spring.boot.security.test)
	testImplementation(libs.spring.boot.starter.log4j2)
}

// Es una librería: no tiene clase main y se consume como dependencia de proyecto.
tasks.named<BootJar>("bootJar") {
	enabled = false
}

tasks.named<Jar>("jar") {
	enabled = true
}
