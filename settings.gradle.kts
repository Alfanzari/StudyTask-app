pluginManagement {

    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }

    plugins {

        id("com.android.application") version "8.7.3"

        id("org.jetbrains.kotlin.android") version "2.0.20"

        id("org.jetbrains.kotlin.plugin.compose") version "2.0.20"

        id("com.google.devtools.ksp") version "2.0.20-1.0.25"
    }
}

dependencyResolutionManagement {

    repositoriesMode.set(
        RepositoriesMode.FAIL_ON_PROJECT_REPOS
    )

    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "StudyTask"

include(":app")