plugins {
    id("com.android.application")
    id("com.github.triplet.play") version "3.1.0"
    kotlin("android")
    kotlin("kapt")
    id("io.objectbox")
    id("com.hiya.jacoco-android")
}

val versionMajor = 2
val versionMinor = 3
val versionPatch = 1
//TODO need to increment this manually at the moment, as GPP is broken
val versionBuild = 0 // This value is managed by the gradle publisher plugin. Build numbers get incremented on publish
val googleMapsAPIKey = extra.get("google_maps_api_key")?.toString() ?: "PLACEHOLDER_API_KEY"

android {
    compileSdkVersion(30)

    defaultConfig {
        applicationId = "org.owntracks.android"
        minSdkVersion(21)
        targetSdkVersion(30)

        versionCode = versionMajor * 10000 + versionMinor * 1000 + versionPatch * 100 + versionBuild
        versionName = "${versionMajor}.${versionMinor}.${versionPatch}"

        javaCompileOptions {
            annotationProcessorOptions {
                arguments(mapOf("eventBusIndex" to "org.owntracks.android.EventBusIndex"))
            }
        }
        val locales = listOf("en", "de", "fr", "es", "ru", "ca")
        buildConfigField("String[]", "TRANSLATION_ARRAY", "new String[]{\"" + locales.joinToString("\",\"") + "\"}")
        resConfigs(locales)
        testInstrumentationRunner("androidx.test.runner.AndroidJUnitRunner")
/* TODO Get this lot sorted when the orchestrator / coverage / clearPackageData bug gets fixed */
//        testInstrumentationRunnerArguments clearPackageData: "true"
//        testInstrumentationRunnerArguments coverageFilePath: "/sdcard/"
//        testInstrumentationRunnerArguments coverage: "true"
    }

    signingConfigs {
        register("release") {
            keyAlias = "upload"
            keyPassword = System.getenv("KEYSTORE_PASSPHRASE")
            storeFile = file("../owntracks.release.keystore.jks")
            storePassword = System.getenv("KEYSTORE_PASSPHRASE")
        }
    }

    buildTypes {

        named("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            isZipAlignEnabled = true
            proguardFiles = mutableListOf(getDefaultProguardFile("proguard-android.txt"), file("proguard-rules.pro"))
            resValue("string", "GOOGLE_MAPS_API_KEY", googleMapsAPIKey)
            signingConfig = signingConfigs.findByName("release")
        }

        named("debug") {
            isMinifyEnabled = false
            isShrinkResources = false
            isZipAlignEnabled = true
            proguardFiles = mutableListOf(getDefaultProguardFile("proguard-android.txt"), file("proguard-rules.pro"))
            resValue("string", "GOOGLE_MAPS_API_KEY", googleMapsAPIKey)
            applicationIdSuffix = ".debug"
            isTestCoverageEnabled = true
        }
    }

    buildFeatures {
        dataBinding = true
        viewBinding = true
    }

    packagingOptions {
        exclude("META-INF/DEPENDENCIES.txt")
        exclude("META-INF/LICENSE.txt")
        exclude("META-INF/NOTICE.txt")
        exclude("META-INF/NOTICE")
        exclude("META-INF/LICENSE")
        exclude("META-INF/DEPENDENCIES")
        exclude("META-INF/notice.txt")
        exclude("META-INF/license.txt")
        exclude("META-INF/dependencies.txt")
        exclude("META-INF/LGPL2.1")
        exclude("META-INF/proguard/androidx-annotations.pro")
    }

    lintOptions {
        baselineFile = file("../../lint/lint-baseline.xml")
        isCheckAllWarnings = true
        isWarningsAsErrors = false
        isAbortOnError = false
        disable("TypographyFractions", "TypographyQuotes", "Typos", "UnsafeExperimentalUsageError", "UnsafeExperimentalUsageWarning")
    }
    testOptions {
        animationsDisabled = true
        unitTests {
            isIncludeAndroidResources = true
            isIncludeAndroidResources = true
        }
//        execution "ANDROIDX_TEST_ORCHESTRATOR"
    }

    tasks.withType<Test> {
        testLogging {
            events("passed", "skipped", "failed")
            setExceptionFormat("full")
        }
        reports.junitXml.isEnabled = true
        reports.html.isEnabled = false
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_1_8.toString()
    }
    buildToolsVersion = "29.0.3"
}

kapt {
    correctErrorTypes = true
}

val daggerVersion = "2.33"
val okHttpVersion = "4.9.1"
val jacksonVersion = "2.12.2"
val materialDialogsVersion = "0.9.6.0"
val espressoVersion = "3.3.0"
val androidxTestVersion = "1.3.0"
val kotlinCoroutinesVersion = "1.4.1"

dependencies {
    implementation("androidx.preference:preference:1.1.1")
    implementation("com.takisoft.preferencex:preferencex:1.1.0")
    implementation("com.google.android.material:material:1.3.0")
    implementation("androidx.work:work-runtime:2.5.0")

    // Explicit dependency on conscrypt to give up-to-date TLS support on all devices
    implementation("org.conscrypt:conscrypt-android:2.5.2")

    // Play Services libraries
    implementation("com.google.android.gms:play-services-maps:17.0.0")
    implementation("com.google.android.gms:play-services-location:18.0.0")

    // Utility libraries
    implementation("com.google.dagger:dagger:${daggerVersion}")
    implementation("com.google.dagger:dagger-android-support:${daggerVersion}")
    implementation("com.google.dagger:dagger-android:${daggerVersion}")

    implementation("org.greenrobot:eventbus:3.2.0")
    implementation("org.eclipse.paho:org.eclipse.paho.client.mqttv3:1.2.5")

    implementation("com.squareup.okhttp3:okhttp:${okHttpVersion}")
    implementation("com.squareup.okhttp3:logging-interceptor:${okHttpVersion}")

    implementation("com.fasterxml.jackson.core:jackson-core:${jacksonVersion}")
    implementation("com.fasterxml.jackson.core:jackson-databind:${jacksonVersion}")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:${jacksonVersion}")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:${jacksonVersion}")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:${kotlinCoroutinesVersion}")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:${kotlinCoroutinesVersion}")

    implementation("com.jakewharton.timber:timber:4.7.1")
    implementation("com.github.joshjdevl.libsodiumjni:libsodium-jni-aar:2.0.2")
    implementation("org.apache.httpcomponents.core5:httpcore5:5.1")
    implementation("commons-codec:commons-codec:1.15")

    implementation("org.threeten:threetenbp:1.5.0")
    implementation("com.github.joschi.jackson:jackson-datatype-threetenbp:$jacksonVersion")

    // Widget libraries
    implementation("com.rengwuxian.materialedittext:library:2.1.4")
    implementation("com.mikepenz:materialdrawer:6.1.2@aar") { isTransitive = true }
    implementation("com.mikepenz:materialize:1.2.1@aar")
    implementation( "no.nordicsemi.android.support.v18:scanner:1.4.2")








    // Java language implementation
//    implementation ("androidx.core:core:1.4.10")
    // Kotlin
    implementation ("androidx.core:core-ktx:1.3.2")

    // To use RoleManagerCompat
//    implementation ("androidx.core:core-role:1.0.0")

    // To use the Animator APIs
//    implementation ("androidx.core:core-animation:1.0.0-alpha02")
    // To test the Animator APIs










    implementation("androidx.test.espresso:espresso-idling-resource:${espressoVersion}")
    implementation ("org.altbeacon:android-beacon-library:2.+")
    implementation ("com.jakewharton:butterknife:10.2.1")
    annotationProcessor ("com.jakewharton:butterknife-compiler:10.2.1")
    implementation ("no.nordicsemi.android:ble-livedata:2.2.0")

    // These Java EE libs are no longer included in JDKs, so we include explicitly
    kapt("javax.xml.bind:jaxb-api:2.3.1")
    kapt("com.sun.xml.bind:jaxb-core:2.3.0.1")
    kapt("com.sun.xml.bind:jaxb-impl:2.3.2")

    // Preprocessors
    kapt("org.greenrobot:eventbus-annotation-processor:3.2.0")
    kapt("com.google.dagger:dagger-compiler:${daggerVersion}")
    kapt("com.google.dagger:dagger-android-processor:${daggerVersion}")

    kaptTest("com.google.dagger:dagger-compiler:${daggerVersion}")
    kaptTest("com.google.dagger:dagger-android-processor:${daggerVersion}")

    testImplementation("androidx.test:core:${androidxTestVersion}")
    testImplementation("com.nhaarman.mockitokotlin2:mockito-kotlin:2.2.0")
    testImplementation("android.arch.core:core-testing:1.1.1")

    androidTestImplementation("androidx.test.espresso:espresso-core:${espressoVersion}")
    androidTestImplementation("androidx.test.espresso:espresso-contrib:${espressoVersion}")
    androidTestImplementation("androidx.test.espresso:espresso-intents:${espressoVersion}")

    androidTestImplementation("androidx.test.ext:junit:1.1.2")
    androidTestImplementation("androidx.test:core-ktx:${androidxTestVersion}")
    androidTestImplementation("com.schibsted.spain:barista:3.9.0") {
        exclude("org.jetbrains.kotlin")
    }
    androidTestImplementation("com.squareup.okhttp3:mockwebserver:${okHttpVersion}")

    androidTestImplementation("androidx.test:rules:${androidxTestVersion}")
    androidTestImplementation("androidx.test:runner:${androidxTestVersion}")
    androidTestUtil("androidx.test:orchestrator:${androidxTestVersion}")
}


// Publishing
val serviceAccountCreds = file("owntracks-android-7a8e1517bde3.json")

play {
    if (serviceAccountCreds.exists()) {
        enabled.set(true)
        serviceAccountCredentials.set(serviceAccountCreds)
    } else {
        enabled.set(false)
    }
    track.set("internal")

    resolutionStrategy.set(com.github.triplet.gradle.androidpublisher.ResolutionStrategy.AUTO)
}

// Espresso test  screenshot gathering
val reportsDirectory = File("$buildDir/reports/androidTests/connected/flavors/debugAndroidTest")
val screenshotsDeviceFolder = "/storage/emulated/0/Download/testscreenshots"

val createScreenshotDirectoryTask = tasks.register<Exec>("createScreenshotDirectory") {
    group = "reporting"
    description = "Creates screenshot directory on connected device"
    executable = "${android.adbExecutable}"
    args(mutableListOf("shell", "mkdir", "-p", screenshotsDeviceFolder))
}

val embedScreenshotsTask = tasks.register("embedScreenshots") {
    group = "reporting"
    description = "Embeds the screenshots in the test report"
    doFirst {
        val screenshotsDirectory = File(reportsDirectory, "testscreenshots/")
        if (!screenshotsDirectory.exists()) {
            println("Could not find screenshots. Skipping...")
            return@doFirst
        }
        screenshotsDirectory
                .listFiles()!!
                .forEach { testClassDirectory ->
                    val testClassName = testClassDirectory.name
                    testClassDirectory.listFiles()?.forEach failedFile@{
                        val testName = it.name
                        val testNameWithoutExtension = it.nameWithoutExtension
                        val testClassJunitReportFile = File(reportsDirectory, "${testClassName}.html")
                        if (!testClassJunitReportFile.exists()) {
                            println("Could not find JUnit report file for test class '${testClassJunitReportFile}'")
                            return@failedFile
                        }
                        val testJunitReportContent = testClassJunitReportFile.readText()

                        val failedHeaderPatternToFind = "<h3 class=\"failures\">${testNameWithoutExtension}</h3>"

                        val failedPatternToReplace = "$failedHeaderPatternToFind <img src=\"testscreenshots/${testClassName}/${testName}\" width =\"360\" />"
                        val successRecordPatternToFind = "<td>${testNameWithoutExtension}</td>"
                        val successPatternToReplace = "<td>${testNameWithoutExtension} <a href=\"testscreenshots/${testClassName}/${testName}\">(screenshot)</a></td>"

                        testClassJunitReportFile.writeText(testJunitReportContent
                                .replace(failedHeaderPatternToFind, failedPatternToReplace)
                                .replace(successRecordPatternToFind, successPatternToReplace)
                        )
                    }
                }
    }
}


val fetchScreenshotsTask = tasks.register<Exec>("fetchScreenshots") {
    group = "reporting"
    description = "Fetches espresso screenshots from the device"
    executable = "${android.adbExecutable}"
    args("pull", screenshotsDeviceFolder, reportsDirectory.toString())
    finalizedBy(clearScreenshotsTask)
    dependsOn(createScreenshotDirectoryTask)
    doFirst {
        reportsDirectory.mkdirs()
    }
}
val clearScreenshotsTask = tasks.register<Exec>("clearScreenshots") {
    group = "reporting"
    description = "Removes screenshots from connected device"
    executable = "${android.adbExecutable}"
    args("shell", "rm", "-rf", screenshotsDeviceFolder)
    finalizedBy(embedScreenshotsTask)
}

tasks.whenTaskAdded {
    if (name == "connectedDebugAndroidTest") {
        finalizedBy(fetchScreenshotsTask)
    }
}