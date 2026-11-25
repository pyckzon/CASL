plugins {
	id("org.springframework.boot") apply false
	id("io.spring.dependency-management") apply false
	// Aplicado sin versión: el plugin lo aporta buildSrc (mismo patrón que los dos
	// anteriores). Pedirlo con versión (alias) falla en Gradle 9.9.2 con
	// "already on the classpath with an unknown version". La versión queda fijada
	// en gradle/libs.versions.toml (cyclonedx = 3.6.1) y en buildSrc.
	id("org.cyclonedx.bom")
	id("base")
}

