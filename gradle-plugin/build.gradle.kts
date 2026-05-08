plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.buildconfig)
    alias(libs.plugins.gradle.plugin)
    alias(libs.plugins.maven.publish)
}

version = "${libs.versions.kotlin.compiler.get()}-$version"

sourceSets {
    main {
        java.setSrcDirs(listOf("src"))
        resources.setSrcDirs(listOf("resources"))
    }
}

dependencies {
    implementation(libs.kotlin.gradle.plugin.api)
}

buildConfig {
    packageName(project.group.toString())

    buildConfigField("String", "KOTLIN_PLUGIN_ID", "\"${rootProject.group}\"")

    val pluginProject = project(":kt-invoke-control-plugin")
    buildConfigField("String", "KOTLIN_PLUGIN_GROUP", "\"${pluginProject.group}\"")
    buildConfigField("String", "KOTLIN_PLUGIN_NAME", "\"${pluginProject.name}\"")
    // possible that the plugin project hasn't configured its version yet
    buildConfigField("String", "KOTLIN_PLUGIN_VERSION", "\"$version\"")

    val annotationsProject = project(":kt-invoke-control-lib")
    buildConfigField(
        type = "String",
        name = "ANNOTATIONS_LIBRARY_COORDINATES",
        expression = "\"${annotationsProject.group}:${annotationsProject.name}:${annotationsProject.version}\""
    )
}

gradlePlugin {
    plugins {
        create("InvokeControl") {
            id = rootProject.group.toString()
            displayName = "InvokeControl"
            description = "endoleon's malevolent perturbation!"
            implementationClass = "st.evening.kt.invokecontrol.ICGradlePlugin"
        }
    }
}
