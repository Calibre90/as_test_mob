plugins { id("com.android.application") }
android {
    namespace = "com.calibre90.catalogadmin"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.calibre90.catalogadmin"
        minSdk = 26
        targetSdk = 35
        versionCode = (project.findProperty("buildNumber")?.toString()?.toIntOrNull() ?: 1)
        versionName = "0.1.0-draft"
    }
}

// Reuse the original Run #303 administrator implementation and its resources.
android.sourceSets.getByName("main").java.srcDir("../app/src/main/java")
android.sourceSets.getByName("main").res.srcDir("../app/src/main/res")
