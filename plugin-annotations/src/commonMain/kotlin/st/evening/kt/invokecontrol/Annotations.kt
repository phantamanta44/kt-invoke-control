package st.evening.kt.invokecontrol

/**
 * Marks an annotation class as a restriction annotation, meaning it restricts access to annotated declarations to
 * scopes that have the specified permissions.
 *
 * If a function type is annotated as such, it means that functions of that type can only be called with the specified
 * permissions. An anonymous function may be annotated to restrict its type, thus granting the permissions within the
 * scope of the function body.
 *
 * Simple string interpolation of annotation arguments is supported using the `${paramName}` syntax.
 */
@Target(AnnotationTarget.ANNOTATION_CLASS)
@Retention(AnnotationRetention.BINARY)
@MustBeDocumented
public annotation class ICRestrictAnnotation(vararg val permissions: String)

/**
 * Directly restricts declarations with a given set of permissions.
 */
@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.PROPERTY,
    AnnotationTarget.FIELD,
    AnnotationTarget.CONSTRUCTOR,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER,
    AnnotationTarget.TYPE, // only for function types
    AnnotationTarget.FILE
)
@Retention(AnnotationRetention.BINARY)
@MustBeDocumented
public annotation class ICRestrict(vararg val permissions: String)

/**
 * Instructs the permission checker to ignore the given permissions when checking the annotated expression.
 */
@Target(AnnotationTarget.EXPRESSION)
@Retention(AnnotationRetention.SOURCE)
public annotation class ICUnchecked(vararg val permissions: String)
