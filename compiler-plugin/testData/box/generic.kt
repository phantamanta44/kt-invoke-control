package foo.bar

import st.evening.kt.invokecontrol.ICRestrictAnnotation
import st.evening.kt.invokecontrol.ICUnchecked

@ICRestrictAnnotation("enclave")
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.EXPRESSION, AnnotationTarget.TYPE)
@Retention(AnnotationRetention.SOURCE)
annotation class Enclave

fun box(): String {
    val f = identity<@Enclave () -> String>(::ok)
    return (@ICUnchecked("enclave") f())
}

@Enclave
fun ok(): String = "OK"

fun <T> identity(x: T): T = x
