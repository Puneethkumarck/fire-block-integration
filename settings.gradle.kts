pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

rootProject.name = "custody-fireblocks"

include("custody-fireblocks-api")
include("custody-fireblocks-client")
include("custody-fireblocks")
