plugins {
	`kotlin-dsl`
}

repositories {
	// Del adapter: org.cyclonedx:cyclonedx-gradle-plugin se publica en el Gradle
	// Plugin Portal; en Central devuelve 404 (y a veces 429 en runners nuevos).
	gradlePluginPortal()
	mavenCentral()
}

dependencies {
	implementation("org.springframework.boot:spring-boot-gradle-plugin:4.2.0")
	implementation("io.spring.gradle:dependency-management-plugin:1.1.7")
	implementation("org.cyclonedx:cyclonedx-gradle-plugin:3.6.1")
}

