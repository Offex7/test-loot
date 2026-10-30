pluginManagement { repositories { google(); mavenCentral(); gradlePluginPortal() } }
dependencyResolutionManagement { repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS); repositories { google(); mavenCentral() } }
rootProject.name = "StreamHub"
include(":app", ":tvremote-core", ":tvremote-ui", ":tvremote-companion", ":tvremote-cast")
