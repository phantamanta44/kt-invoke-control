// RUN_PIPELINE_TILL: FRONTEND

package foo.bar

import st.evening.kt.invokecontrol.ICRestrictAnnotation

@ICRestrictAnnotation("enclave")
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.EXPRESSION, AnnotationTarget.TYPE)
@Retention(AnnotationRetention.SOURCE)
annotation class Enclave

fun box(): String {
    return foo @Enclave { flip(it) }
}

fun foo(f: @Enclave (String) -> String): String {
    return <!KIC_INSUFFICIENT_PERMISSIONS!>f("KO")<!>
}

@Enclave
fun flip(x: String): String = x.reversed()
