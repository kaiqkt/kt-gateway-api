plugins {
	kotlin("jvm") version "2.2.21"
	kotlin("plugin.spring") version "2.2.21"
	id("org.springframework.boot") version "3.5.7"
	id("io.spring.dependency-management") version "1.1.7"
    id("jacoco")
}

group = "com.kaiqkt"
version = "0.0.1-SNAPSHOT"
description = "Demo project for Spring Boot"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(21)
	}
}

jacoco {
    toolVersion = "0.8.12"
}

repositories {
	mavenCentral()
}

extra["springCloudVersion"] = "2025.0.0"

dependencies {
	implementation("org.jetbrains.kotlin:kotlin-reflect")
	implementation("org.springframework.cloud:spring-cloud-starter-gateway-server-webmvc")
    implementation("net.logstash.logback:logstash-logback-encoder:7.4")
    implementation("ch.qos.logback:logback-classic:1.5.20")
    implementation("com.github.kittinunf.fuel:fuel:2.3.1")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")

    testImplementation("org.testcontainers:testcontainers:1.20.1")
    testImplementation("org.testcontainers:junit-jupiter:1.20.1")
    testImplementation("com.redis:testcontainers-redis:2.2.4")
    testImplementation("org.mock-server:mockserver-netty-no-dependencies:5.15.0")
    testImplementation("org.mock-server:mockserver-client-java-no-dependencies:5.15.0") {
        exclude("org.sl4j", "slf4j-jdk14")
    }
    testImplementation("io.rest-assured:rest-assured")
    testImplementation("io.mockk:mockk:1.13.14")
	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

dependencyManagement {
	imports {
		mavenBom("org.springframework.cloud:spring-cloud-dependencies:${property("springCloudVersion")}")
	}
}

kotlin {
	compilerOptions {
		freeCompilerArgs.addAll("-Xjsr305=strict")
	}
}

tasks.withType<Test> {
	useJUnitPlatform()
}

val excludePackages: List<String> by extra {
    listOf(
        "com/kaiqkt/gateway/Application*",
    )
}


@Suppress("UNCHECKED_CAST")
fun ignorePackagesForReport(jacocoBase: JacocoReportBase) {
    jacocoBase.classDirectories.setFrom(
        sourceSets.main.get().output.asFileTree.matching {
            exclude(jacocoBase.project.extra.get("excludePackages") as List<String>)
        }
    )
}

tasks.withType<JacocoReport> {
    reports {
        html.required.set(true)
    }
    ignorePackagesForReport(this)
}


tasks.withType<JacocoCoverageVerification> {
    violationRules {
        rule {
            limit {
                minimum = "1.0".toBigDecimal()
                counter = "LINE"
            }
            limit {
                minimum = "1.0".toBigDecimal()
                counter = "BRANCH"
            }
        }
    }
    ignorePackagesForReport(this)
}

tasks.test {
    finalizedBy(tasks.jacocoTestReport, tasks.jacocoTestCoverageVerification)
}
