package st.evening.kt.invokecontrol.kplugin.permission

import org.jetbrains.kotlin.AbstractKtSourceElement
import org.jetbrains.kotlin.diagnostics.DiagnosticContext
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.psi.psiUtil.isIdentifier
import st.evening.kt.invokecontrol.kplugin.ICDiagnostics

class PermissionFactory private constructor(
    private val segments: List<Segment>,
    private val templateSource: AbstractKtSourceElement?
) {
    companion object {
        private val ARG_TEMPLATE_PATTERN: Regex = Regex("""\$\{([^}]*)}""", RegexOption.IGNORE_CASE)

        context(context: DiagnosticContext, reporter: DiagnosticReporter)
        fun fromTemplate(templateString: String, templateSource: AbstractKtSourceElement?): PermissionFactory? {
            val segments = mutableListOf<Segment>()
            var prevMatchEnd = 0
            var match = ARG_TEMPLATE_PATTERN.find(templateString)
            while (match != null) {
                val matchStart = match.range.first
                if (matchStart > prevMatchEnd) {
                    segments += Segment.Literal(templateString.substring(prevMatchEnd, matchStart))
                }
                val key = match.groupValues[1]
                if (!key.isIdentifier()) {
                    reporter.reportOn(templateSource, ICDiagnostics.KIC_INVALID_PERMISSION_ARGUMENT_KEY, key)
                    return null
                }
                segments += Segment.Argument(key)
                prevMatchEnd = match.range.last + 1
                match = match.next()
            }
            if (prevMatchEnd < templateString.length) {
                segments += Segment.Literal(templateString.substring(prevMatchEnd))
            }
            return PermissionFactory(segments, templateSource)
        }
    }

    context(context: DiagnosticContext, reporter: DiagnosticReporter)
    fun build(argumentProvider: PermissionArgumentProvider): String? = buildString {
        segments.forEach {
            append(
                when (it) {
                    is Segment.Argument -> argumentProvider.get(it.key, templateSource) ?: return null
                    is Segment.Literal -> it.value
                }
            )
        }
    }

    private sealed interface Segment {
        class Argument(val key: String) : Segment
        class Literal(val value: String) : Segment
    }
}

fun interface PermissionArgumentProvider {
    context(context: DiagnosticContext, reporter: DiagnosticReporter)
    fun get(key: String, useSite: AbstractKtSourceElement?): String?
}
