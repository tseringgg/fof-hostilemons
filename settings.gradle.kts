 rootProject.name = "fightorflight"

pluginManagement.repositories {
    maven("https://maven.fabricmc.net/")
    maven("https://maven.architectury.dev/")
    //maven("https://maven.minecraftforge.net/")
    maven("https://maven.neoforged.net/releases/")
    gradlePluginPortal()
}

include("common", "fabric", "neoforge")