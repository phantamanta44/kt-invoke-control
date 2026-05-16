package st.evening.kt.invokecontrol.kplugin.permission

import org.jetbrains.kotlin.AbstractKtSourceElement
import org.jetbrains.kotlin.diagnostics.DiagnosticContext
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.psi.psiUtil.isIdentifier
import st.evening.kt.invokecontrol.kplugin.ICDiagnostics

class Permission private constructor(val segments: List<Segment>, val source: AbstractKtSourceElement?) {
    companion object {
        private val ARG_TEMPLATE_PATTERN: Regex = Regex("""\$\{([^}]*)}""", RegexOption.IGNORE_CASE)

        context(context: DiagnosticContext, reporter: DiagnosticReporter)
        fun fromTemplate(templateString: String, templateSource: AbstractKtSourceElement?): Permission? =
            parseTemplate(templateString, templateSource)?.let { Permission(it, templateSource) }

        fun fromSegments(segments: List<Segment>, source: AbstractKtSourceElement?): Permission =
            Permission(compact(segments), source)

        fun ofLiteral(value: String, source: AbstractKtSourceElement?): Permission =
            Permission(listOf(Segment.Literal(value)), source)

        fun ofVariable(key: String, source: AbstractKtSourceElement?): Permission =
            Permission(listOf(Segment.Variable(key)), source)

        context(context: DiagnosticContext, reporter: DiagnosticReporter)
        fun parseTemplate(templateString: String, templateSource: AbstractKtSourceElement?): List<Segment>? {
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
                segments += Segment.Variable(key)
                prevMatchEnd = match.range.last + 1
                match = match.next()
            }
            if (prevMatchEnd < templateString.length) {
                segments += Segment.Literal(templateString.substring(prevMatchEnd))
            }
            return segments
        }

        fun compact(segments: List<Segment>): List<Segment> {
            if (segments.size <= 1) return segments
            val compacted = mutableListOf<Segment>()
            var literalRun: StringBuilder? = null
            segments.forEach {
                when (it) {
                    is Segment.Variable -> {
                        if (literalRun != null) {
                            compacted += Segment.Literal(literalRun.toString())
                            literalRun = null
                        }
                        compacted += it
                    }

                    is Segment.Literal -> {
                        if (literalRun == null) {
                            literalRun = StringBuilder(it.value)
                        } else {
                            literalRun.append(it.value)
                        }
                    }
                }
            }
            if (literalRun != null) {
                compacted += Segment.Literal(literalRun.toString())
            }
            return compacted
        }

        context(_: DiagnosticContext)
        fun substituteSegments(
            segments: List<Segment>,
            substitution: Substitution,
            source: AbstractKtSourceElement?
        ): List<Segment>? {
            val result = mutableListOf<Segment>()
            segments.forEach {
                when (it) {
                    is Segment.Variable -> result.addAll(substitution.substitute(it.key, source) ?: return null)
                    is Segment.Literal -> result += it
                }
            }
            return compact(result)
        }
    }

    context(_: DiagnosticContext)
    fun substitute(substitution: Substitution): Permission? =
        substituteSegments(segments, substitution, source)?.let { Permission(it, source) }

    override fun toString(): String = segments.joinToString("")

    override fun equals(other: Any?): Boolean = this === other || other is Permission && segments == other.segments

    override fun hashCode(): Int = segments.hashCode()

    sealed interface Segment {
        data class Variable(val key: String) : Segment {
            override fun toString(): String = $$"${$$key}"
        }

        data class Literal(val value: String) : Segment {
            override fun toString(): String = value
        }
    }

    fun interface Substitution {
        context(_: DiagnosticContext)
        fun substitute(key: String, source: AbstractKtSourceElement?): List<Segment>?
    }
}
