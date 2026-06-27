package st.evening.kt.invokecontrol.services

import org.jetbrains.kotlin.test.directives.model.SimpleDirectivesContainer

object ICTestDirectives : SimpleDirectivesContainer() {
    val IC_RESTRICT_ANNOTATION by stringDirective("Declare restriction annotations")
}
