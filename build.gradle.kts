description = "project for RNIS EGTS packets encoding-decoding demo"

plugins {
    alias(libs.plugins.kapt)
    alias(libs.plugins.kotlin)
}

subprojects {
    apply(plugin = "kotlin")
    apply(plugin = "kotlin-kapt")
    apply(plugin = "jacoco")

    group = "tech.ecom.egts"

    if (tasks.findByName("test") != null) {
        tasks.named<Test>("test") {
            group = "tests"
            useJUnitPlatform()
            testLogging {
                events("passed", "skipped", "failed")
            }
            finalizedBy(tasks.named("jacocoTestReport"))
        }
    }
}
