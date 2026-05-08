// RUN_PIPELINE_TILL: FRONTEND

package foo.bar

import st.evening.kt.invokecontrol.ICRestrictAnnotation

@ICRestrictAnnotation("enclave")
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY, AnnotationTarget.EXPRESSION, AnnotationTarget.TYPE)
@Retention(AnnotationRetention.SOURCE)
annotation class Enclave

fun box(): String {
    return runEnclave(<!KIC_LEAKY_ASSIGNMENT!>::foo<!>) + runEnclave(<!KIC_LEAKY_ASSIGNMENT!>::bar<!>)
}

fun <T> runEnclave(f: () -> T): T {
    return f()
}

@Enclave
fun foo(): String = "OK"

@Enclave
val bar: String
    get() = "K"
