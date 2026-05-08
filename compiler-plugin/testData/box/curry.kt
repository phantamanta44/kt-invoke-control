package foo.bar

import st.evening.kt.invokecontrol.ICRestrictAnnotation
import st.evening.kt.invokecontrol.ICUnchecked

@ICRestrictAnnotation("enclave")
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.EXPRESSION, AnnotationTarget.TYPE)
@Retention(AnnotationRetention.SOURCE)
annotation class Enclave

fun box(): String {
    return (@ICUnchecked("enclave") reverser(::concat)("K")("O"))
}

fun reverser(f: @Enclave (String, String) -> String): (String) -> @Enclave (String) -> String {
    return fun(x: String): @Enclave (String) -> String =
        @Enclave { y -> f(y, x) }
}

@Enclave
fun concat(a: String, b: String): String = a + b

fun <T> identity(x: T): T = x
