plugins { id("com.android.application") }
android {
    namespace = "com.calibre90.astestmob"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.calibre90.astestmob"
        minSdk = 26
        targetSdk = 35
        versionCode = (project.findProperty("buildNumber")?.toString()?.toIntOrNull() ?: 1)
        versionName = "1.0." + (project.findProperty("buildNumber")?.toString()?.toIntOrNull() ?: 1).toString()
    }
}
