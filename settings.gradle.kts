// ==============================================================================
// VESSEL-API — Settings Gradle (Kotlin DSL)
// Version: 1.0.0
// Description: Defines the root project name and plugin management repositories
//              for the public VESSEL Universal Super-API (VUSA).
// ==============================================================================

pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}

rootProject.name = "vessel-api"
