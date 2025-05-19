plugins{
    id("dev.architectury.loom")
    id("architectury-plugin")
}

architectury {
    common("neoforge", "fabric")
    platformSetupLoomIde()
}

repositories {
    mavenLocal()
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    modImplementation("com.cobblemon:mod:${project.properties["cobblemon_version"]}")
    implementation(files("../libs/molang/molang-1.1.18.jar"))


    modApi("me.shedaniel.cloth:cloth-config:${project.properties["cloth_config_version"]}")
    modApi("dev.architectury:architectury:${project.properties["architectury_version"]}")
}