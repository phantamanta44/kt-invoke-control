package st.evening.kt.invokecontrol

import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Provider

internal open class ICExtensionImpl(private val project: Project) : ICExtension {
    private class AnnotationSpecImpl(objects: ObjectFactory) : AnnotationSpec {
        override val permissions: ListProperty<String> = objects.listProperty(String::class.java)
    }

    override val annotations: MutableMap<String, AnnotationSpec> = mutableMapOf()

    override fun annotation(annotationClass: String, action: Action<AnnotationSpec>) {
        action.execute(
            annotations.getOrPut(annotationClass) { AnnotationSpecImpl(project.objects) }
        )
    }

    override fun annotation(annotationClass: String, permissions: Provider<Iterable<String>>) {
        annotation(annotationClass) {
            it.permissions.addAll(permissions)
        }
    }

    override fun annotation(annotationClass: String, vararg permissions: Provider<String>) {
        annotation(annotationClass, project.provider { permissions.map { it.get() } }) // could traverse, but whatever
    }

    override fun annotation(annotationClass: String, permissions: Iterable<String>) {
        annotation(annotationClass) {
            it.permissions.addAll(permissions)
        }
    }

    override fun annotation(annotationClass: String, vararg permissions: String) {
        annotation(annotationClass) {
            it.permissions.addAll(*permissions)
        }
    }
}
