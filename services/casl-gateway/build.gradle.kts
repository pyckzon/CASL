import org.springframework.boot.gradle.tasks.bundling.BootBuildImage

plugins {
	id("casl-spring-boot-service")
}

group = "com.casl"
version = "0.0.1-SNAPSHOT"

dependencyManagement {
	imports {
		mavenBom("org.springframework.cloud:spring-cloud-dependencies:${libs.versions.springCloud.get()}")
	}
}

dependencies {
	implementation(libs.spring.boot.starter.log4j2)
	implementation(libs.spring.boot.starter.actuator)
	implementation(libs.spring.boot.starter.oauth2.resource.server)
	implementation(libs.spring.boot.starter.security)
	implementation("org.springframework.cloud:spring-cloud-starter-gateway-server-webmvc")

	developmentOnly(libs.spring.boot.devtools)

	testImplementation(libs.spring.boot.starter.test)
	testImplementation(libs.spring.boot.starter.webmvc.test)
}

tasks.named<BootBuildImage>("bootBuildImage") {
	imageName.set("casl/casl-gateway:0.0.1-SNAPSHOT")
}

// El gateway no contiene lógica de negocio: solo cableado de Spring Cloud.
// El gate de cobertura no aplica a módulos de infraestructura pura.
tasks.named("jacocoTestCoverageVerification") {
	enabled = false
}
