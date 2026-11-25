plugins {
	`kotlin-dsl`
}

repositories {
	mavenCentral()
	// org.cyclonedx:cyclonedx-gradle-plugin se publica solo en el Gradle Plugin Portal.
	gradlePluginPortal()
}

dependencies {
	implementation("org.springframework.boot:spring-boot-gradle-plugin:4.2.0")
	implementation("io.spring.gradle:dependency-management-plugin:1.1.7")
	implementation("org.cyclonedx:cyclonedx-gradle-plugin:3.6.1")
}

