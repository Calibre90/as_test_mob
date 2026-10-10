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
