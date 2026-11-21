pluginManagement {
	repositories {
		mavenCentral()
	}
	plugins {
		id("org.springframework.boot") version "4.1.2"
		id("io.spring.dependency-management") version "1.1.7"
	}
}

rootProject.name = "casl"

include(":services:casl-users-service")

