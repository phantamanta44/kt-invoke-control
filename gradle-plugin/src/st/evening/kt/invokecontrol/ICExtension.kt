package st.evening.kt.invokecontrol

import org.gradle.api.Action
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Provider

interface AnnotationSpec {
    val permissions: ListProperty<String>
}

interface ICExtension {
    val annotations: Map<String, AnnotationSpec>

    fun annotation(annotationClass: String, action: Action<AnnotationSpec>)

    fun annotation(annotationClass: String, permissions: Provider<Iterable<String>>)
    fun annotation(annotationClass: String, vararg permissions: Provider<String>)
    fun annotation(annotationClass: String, permissions: Iterable<String>)
    fun annotation(annotationClass: String, vararg permissions: String)
}
