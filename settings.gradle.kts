pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    // Flutter plugin subprojects may declare their own repositories (from pub cache).
    // Use project repositories preference to avoid repeated Gradle warnings.
    repositoriesMode.set(RepositoriesMode.PREFER_PROJECT)
    val storageUrl: String = System.getenv("FLUTTER_STORAGE_BASE_URL") ?: "https://storage.googleapis.com"
    repositories {
        google()
        mavenCentral()
        maven("$storageUrl/download.flutter.io")
    }
}

rootProject.name = "OpenOmniOperator"
include(":app")

val filePath = settingsDir.toString() + "/flutter_module/.android/include_flutter.groovy"
apply(from = File(filePath))
