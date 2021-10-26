import org.gradle.api.tasks.testing.logging.TestLogEvent.STANDARD_OUT
import org.gradle.api.tasks.testing.logging.TestLogEvent.PASSED
import org.gradle.api.tasks.testing.logging.TestLogEvent.FAILED
import org.gradle.api.tasks.testing.logging.TestLogEvent.SKIPPED

plugins {
    id("org.jlleitschuh.gradle.ktlint")
    id("org.jlleitschuh.gradle.ktlint-idea")

    id("com.github.kt3k.coveralls")

    id("io.github.gradle-nexus.publish-plugin")

    kotlin("jvm")
}

allprojects {
    apply(plugin = "kotlin")
    apply(plugin = "jacoco")

    group = "io.github.pelletier197"

    configure<JavaPluginExtension> {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    repositories {
        mavenCentral()
    }

    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions {
            jvmTarget = "11"
        }
    }

    tasks.withType<Test> {
        useJUnitPlatform()
        testLogging {
            showStandardStreams = true
            events = setOf(STANDARD_OUT, PASSED, SKIPPED, FAILED)
        }
        finalizedBy("jacocoTestReport")
    }
}

configure<io.github.gradlenexus.publishplugin.NexusPublishExtension> {
    repositories {
        sonatype {
            username.set(System.getenv("SONATYPE_NEXUS_USERNAME"))
            password.set(System.getenv("SONATYPE_NEXUS_PASSWORD"))
        }
    }
}

//tasks.withType<JacocoReport>() {
//    executionData.setFrom(files(executionData. { it.exists() }))
//
//    description = 'Generates an aggregate report from all subprojects'
//
//    def coverageProjects = [project(':core'), project(':core-java-tests')]
//    sourceDirectories.setFrom(files(coverageProjects.sourceSets.main.allSource.srcDirs))
//    classDirectories.setFrom(files(coverageProjects.sourceSets.main.output))
//    executionData.setFrom(files(coverageProjects.jacocoTestReport.executionData))
//
//    reports {
//        html {
//            enabled true
//        }
//        xml {
//            enabled true
//        }
//    }
//}
//task jacocoRootReport(type: , group: 'Coverage reports') {
//
//}

coveralls {
//    sourceDirs = listOf(project(":core").["${project(":core").projectDir}/src/main/kotlin"]
    service = "circleci"
//    jacocoReportPath = "${buildDir}/reports/jacoco/jacocoRootReport/jacocoRootReport.xml"
}

tasks.coveralls {
    description = "Uploads coverage report to coveralls"

    dependsOn("jacocoRootReport")
}

