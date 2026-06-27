package st.evening.kt.invokecontrol.services

import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.directives.model.DirectivesContainer
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.EnvironmentConfigurator
import org.jetbrains.kotlin.test.services.TestServices
import st.evening.kt.invokecontrol.kplugin.ICCommandLineProcessor
import st.evening.kt.invokecontrol.kplugin.ICComponentRegistrar

fun TestConfigurationBuilder.configurePlugin() {
    useConfigurators(::ExtensionRegistrarConfigurator)
    configureAnnotations()
}

private class ExtensionRegistrarConfigurator(testServices: TestServices) : EnvironmentConfigurator(testServices) {
    override val directiveContainers: List<DirectivesContainer>
        get() = listOf(ICTestDirectives)
    private val registrar = ICComponentRegistrar()

    override fun configureCompilerConfiguration(configuration: CompilerConfiguration, module: TestModule) {
        module.directives[ICTestDirectives.IC_RESTRICT_ANNOTATION].forEach { directive ->
            ICCommandLineProcessor.processOption(configuration, directive)
        }
    }

    override fun CompilerPluginRegistrar.ExtensionStorage.registerCompilerExtensions(
        module: TestModule,
        configuration: CompilerConfiguration
    ) {
        with(registrar) { registerExtensions(configuration) }
    }
}
