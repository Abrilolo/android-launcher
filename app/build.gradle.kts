plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}
android {
    namespace = "com.adrian.minimallauncher"
    compileSdk = 37
    defaultConfig {
        applicationId = "com.adrian.minimallauncher"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "0.1.0"
    }
    buildFeatures { compose = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
// Reuse the reviewed SVG/gesture engine, but package a local production document with native data.
val sessionAssets = layout.buildDirectory.dir("generated/exerciseSessionAssets")
val generateExerciseSessionAssets = tasks.register("generateExerciseSessionAssets") {
    val prototype = rootProject.file("docs/design/concept/EjercicioSesionVisual.html")
    val runtime = file("src/main/session/runtime.js")
    val style = file("src/main/session/runtime.css")
    inputs.files(prototype, runtime, style)
    outputs.dir(sessionAssets)
    doLast {
        var html = prototype.readText(Charsets.UTF_8)
        html = html.replace(Regex("<link[^>]*>"), "")
            .replace(Regex("<h2>EJERCICIO[^<]*</h2>"), "")
        val side = html.indexOf("<div class=\"side\">")
        val script = html.indexOf("<script>", side)
        check(side >= 0 && script > side)
        html = html.removeRange(side, script)
        html = html.replace("<script>", "<script>\nconst HOST = JSON.parse(AndroidSession.load());\n")
        html = html.replace(Regex("const GROUPS = .*?;"), "const GROUPS = HOST.groups;")
        val dataStart = html.indexOf("const EXS = [")
        val dataEnd = html.indexOf("EXS.forEach", dataStart)
        check(dataStart > 0 && dataEnd > dataStart)
        html = html.replaceRange(dataStart, dataEnd, "const EXS = HOST.exercises;\n")
            .replace("EXS.forEach((e, i) => e.id = 'exercise-' + i);", "")
            .replace("const CUSTOM = { warm: [], exercise: [] };", "const CUSTOM = HOST.library?.custom || { warm: [], exercise: [] };")
        val clockStart = html.indexOf("// ---------- Clock ----------")
        val boot = html.lastIndexOf("go('start');")
        check(clockStart > 0 && boot > clockStart)
        html = html.replaceRange(clockStart, boot + "go('start');".length, runtime.readText(Charsets.UTF_8))
        html = html.replace("</head>", "<meta http-equiv=\"Content-Security-Policy\" content=\"default-src 'none'; script-src 'unsafe-inline'; style-src 'unsafe-inline'; img-src data:; form-action 'none'; base-uri 'none'\"><style>${style.readText(Charsets.UTF_8)}</style></head>")
        val target = sessionAssets.get().file("exercise-session/index.html").asFile
        target.parentFile.mkdirs()
        target.writeText(html, Charsets.UTF_8)
    }
}
android.sourceSets.getByName("main").assets.directories.add(sessionAssets.get().asFile.path)
tasks.named("preBuild").configure { dependsOn(generateExerciseSessionAssets) }
// Room writes each version's schema here, so the next migration can be checked against it.
ksp { arg("room.schemaLocation", "$projectDir/schemas") }
dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2026.09.00")
    implementation(composeBom)
    androidTestImplementation(composeBom)
    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.11.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.11.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.10.2")
    implementation("androidx.datastore:datastore-preferences:1.1.7")
    implementation("androidx.biometric:biometric-ktx:1.2.0-alpha05")
    // biometric pulls fragment 1.2.5, whose FragmentActivity rejects the request codes of the activity-result API (file pickers).
    implementation("androidx.fragment:fragment-ktx:1.8.9")
    // Geofences for casa and oficina (routine phases salida and casa).
    implementation("com.google.android.gms:play-services-location:21.3.0")
    implementation("androidx.room:room-runtime:2.8.5")
    implementation("androidx.room:room-ktx:2.8.5")
    ksp("androidx.room:room-compiler:2.8.5")
    debugImplementation("androidx.compose.ui:ui-tooling")
    testImplementation("junit:junit:4.13.2")
}
