pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        maven(url = "https://jitpack.io")
    }
}

@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { 
            url = uri("https://github.com/jitsi/jitsi-maven-repository/raw/master/releases")
        }
        maven { 
            url = uri("https://jitpack.io")
            credentials { username = "jp_v6v0h1hg3qj0q11tn8mg8fa2rm" }
        }
    }
}

rootProject.name = "AkherApp"
include(":app")
