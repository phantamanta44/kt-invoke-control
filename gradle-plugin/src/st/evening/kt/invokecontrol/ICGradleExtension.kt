package st.evening.kt.invokecontrol

open class ICGradleExtension {
    internal val annotations: MutableMap<String, MutableList<String>> = mutableMapOf()

    open fun restrictAnnotation(fqName: String, action: MutableList<String>.() -> Unit) {
        annotations.getOrPut(fqName) { mutableListOf() }.action()
    }

    open fun restrictAnnotation(fqName: String, vararg permissions: String) {
        restrictAnnotation(fqName) { addAll(permissions) }
    }
}
