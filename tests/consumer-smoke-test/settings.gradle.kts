pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        maven(url = uri("__LOCAL_REPO_URL__"))
        google()
        mavenCentral()
    }
}
rootProject.name = "consumer-smoke-test"
