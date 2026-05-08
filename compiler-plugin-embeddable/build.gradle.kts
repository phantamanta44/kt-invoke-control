import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.shadow)
    alias(libs.plugins.maven.publish)
}

version = "${libs.versions.kotlin.compiler.get()}-$version"

class ProvideEmbeddableComponent @Inject constructor(
    private val softwareComponentFactory: SoftwareComponentFactory
) : Plugin<Project> {
    override fun apply(target: Project) {
        target.components.add(softwareComponentFactory.adhoc("embeddable"))
    }
}

apply<ProvideEmbeddableComponent>()

val embedded: Configuration by configurations.creating

dependencies {
    embedded(project(":kt-invoke-control-plugin")) { isTransitive = false }
}

val jarTask = tasks.named<Jar>("jar") {
    enabled = false
    archiveClassifier.set("original")
}

val runtimeElements = configurations.named<Configuration>("runtimeElements") {
    val jarFile = jarTask.get().archiveFile.get().asFile
    artifacts.removeIf { it.file == jarFile }
}

val embeddableTask = tasks.register<ShadowJar>("embeddable") {
    from(jarTask)
    archiveBaseName.set("kt-invoke-control-plugin-embeddable")
    archiveClassifier.unset()

    configurations.add(embedded)
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    relocate("com.intellij", "org.jetbrains.kotlin.com.intellij")

    enableKotlinModuleRemapping.set(false)
}

tasks.named("assemble") {
    dependsOn(embeddableTask)
}

project.artifacts.add("runtimeElements", embeddableTask) {
    builtBy(embeddableTask)
}

val embeddableComponent = components.named<AdhocComponentWithVariants>("embeddable") {
    addVariantsFromConfiguration(runtimeElements.get()) { mapToMavenScope("runtime") }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            artifactId = "kt-invoke-control-plugin-embeddable"
            from(embeddableComponent.get())
        }
    }
}
