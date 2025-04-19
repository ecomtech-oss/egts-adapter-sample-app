dependencies {
    implementation(libs.spring.boot.starter)

    // egts-adapter-starter
    implementation(libs.egts.adapter)

    // tcp messaging
    implementation(libs.spring.boot.starter.integration)
    implementation(libs.spring.integration.ip)

    // REST API
    implementation(libs.spring.boot.starter.web)

    // serialization for logging
    implementation(libs.jackson.kotlin)

    // test
    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.mockk)
    testImplementation(libs.spring.integration.test.support)
    testImplementation(libs.spring.integration.test)
    testImplementation(libs.springmockk)
}