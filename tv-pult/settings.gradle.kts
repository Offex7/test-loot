pluginManagement { repositories { google(); mavenCentral(); gradlePluginPortal() } }
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories { google(); mavenCentral() }
}
rootProject.name = "TV-Pult"
include(":app", ":tvremote-core", ":tvremote-ui", ":tvremote-androidtv", ":tvremote-cast", ":tvremote-samsung", ":tvremote-webos", ":tvremote-roku", ":tvremote-hid")
