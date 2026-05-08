pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        maven {
            name = "IntelliJ Dependencies"
            url = uri("https://packages.jetbrains.team/maven/p/ij/intellij-dependencies")
        }
    }
}

rootProject.name = "kt-invoke-control"

fun includeFrom(name: String, path: String) {
    include(name)
    project(":$name").projectDir = file(path)
}

includeFrom("kt-invoke-control-plugin", "compiler-plugin")
includeFrom("kt-invoke-control-plugin-embeddable", "compiler-plugin-embeddable")
includeFrom("kt-invoke-control-gradle", "gradle-plugin")
includeFrom("kt-invoke-control-lib", "plugin-annotations")
