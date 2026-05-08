package st.evening.kt.invokecontrol.kplugin

import org.jetbrains.kotlin.compiler.plugin.AbstractCliOption
import org.jetbrains.kotlin.compiler.plugin.CliOption
import org.jetbrains.kotlin.compiler.plugin.CommandLineProcessor
import org.jetbrains.kotlin.config.AnalysisFlag
import org.jetbrains.kotlin.config.ApiVersion
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.CompilerConfigurationKey
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.diagnostics.DiagnosticContext
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.KtDiagnostic
import org.jetbrains.kotlin.diagnostics.Severity
import org.jetbrains.kotlin.name.ClassId
import st.evening.kt.invokecontrol.BuildConfig
import st.evening.kt.invokecontrol.kplugin.permission.PermissionFactory

class ICCommandLineProcessor : CommandLineProcessor {
    companion object {
        val RESTRICT_ANNOTATIONS: CompilerConfigurationKey<Map<ClassId, List<PermissionFactory>>> =
            CompilerConfigurationKey.create("kic-restrict-annotation")
    }

    override val pluginId: String
        get() = BuildConfig.KOTLIN_PLUGIN_ID
    override val pluginOptions: Collection<CliOption> = listOf(
        CliOption(
            "restrict-annotation",
            "[annotation]=[permissions]",
            "Marks an annotation as a restrictive annotation with the given permission set.",
            required = false,
            allowMultipleOccurrences = true
        )
    )

    override fun processOption(option: AbstractCliOption, value: String, configuration: CompilerConfiguration) {
        when (option.optionName) {
            "restrict-annotation" -> {
                val parts = value.split('=', limit = 2)
                if (parts.size != 2) {
                    throw IllegalArgumentException("Malformed restrict annotation option: $value")
                }
                val factories = context(CliDiagnosticReporter, CliDiagnosticContext) {
                    parts[1].split(',').map { PermissionFactory.fromTemplate(it, null)!! }
                }
                val annotations = configuration.getMap(RESTRICT_ANNOTATIONS)
                if (annotations is HashMap<*, *>) {
                    annotations[ClassId.fromString(parts[0])] = factories
                } else {
                    val newAnnotations = HashMap(annotations)
                    newAnnotations[ClassId.fromString(parts[0])] = factories
                    configuration.put(RESTRICT_ANNOTATIONS, newAnnotations)
                }
            }

            else -> error("Unexpected option: ${option.optionName}")
        }
    }
}

private object CliDiagnosticReporter : DiagnosticReporter() {
    override var hasErrors: Boolean = false
        private set

    override fun report(diagnostic: KtDiagnostic?, context: DiagnosticContext) {
        if (diagnostic == null) return
        when (diagnostic.severity) {
            Severity.INFO -> println(diagnostic.renderMessage())

            Severity.WARNING, Severity.FIXED_WARNING, Severity.STRONG_WARNING ->
                System.err.println(diagnostic.renderMessage())

            Severity.ERROR -> throw IllegalStateException(diagnostic.renderMessage())
        }
    }
}

private object CliDiagnosticContext : DiagnosticContext {
    override val containingFilePath: String?
        get() = null
    override val languageVersionSettings: LanguageVersionSettings
        get() = StubLanguageVersionSettings

    override fun isDiagnosticSuppressed(diagnostic: KtDiagnostic): Boolean = false
}

private object StubLanguageVersionSettings : LanguageVersionSettings {
    override val apiVersion: ApiVersion
        get() = throw UnsupportedOperationException()
    override val languageVersion: LanguageVersion
        get() = throw UnsupportedOperationException()

    override fun getCustomizedLanguageFeatures(): Map<LanguageFeature, LanguageFeature.State> = emptyMap()
    override fun getFeatureSupport(feature: LanguageFeature): LanguageFeature.State = LanguageFeature.State.DISABLED
    override fun <T> getFlag(flag: AnalysisFlag<T>): T = flag.defaultValue
    override fun isPreRelease(): Boolean = false
}
