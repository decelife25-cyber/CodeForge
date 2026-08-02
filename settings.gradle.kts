pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
    resolutionStrategy {
        eachPlugin {
            when (requested.id.id) {
                "com.android.application" -> useModule("com.android.tools.build:gradle:8.1.0")
                "org.jetbrains.kotlin.android", "org.jetbrains.kotlin.jvm", "org.jetbrains.kotlin.kapt" -> useModule("org.jetbrains.kotlin:kotlin-gradle-plugin:1.9.10")
            }
        }
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        mavenLocal()
    }
}

rootProject.name = "CodeForge"
include(":app")
