// RUN_PIPELINE_TILL: FRONTEND

package foo.bar

import st.evening.kt.invokecontrol.ICRestrictAnnotation

@ICRestrictAnnotation("enclave")
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.EXPRESSION, AnnotationTarget.TYPE)
@Retention(AnnotationRetention.SOURCE)
annotation class Enclave

fun box(): String {
    val f = identity<@Enclave () -> String>(::ok)
    return <!KIC_INSUFFICIENT_PERMISSIONS!>f()<!>
}

@Enclave
fun ok(): String = "OK"

fun <T> identity(x: T): T = x
