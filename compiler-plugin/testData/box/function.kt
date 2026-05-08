package foo.bar

import st.evening.kt.invokecontrol.ICRestrictAnnotation
import st.evening.kt.invokecontrol.ICUnchecked

@ICRestrictAnnotation("enclave")
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.EXPRESSION, AnnotationTarget.TYPE)
@Retention(AnnotationRetention.SOURCE)
annotation class Enclave

fun box(): String {
    return foo @Enclave { flip(it) }
}

fun foo(f: @Enclave (String) -> String): String {
    return (@ICUnchecked("enclave") f("KO"))
}

@Enclave
fun flip(x: String): String = x.reversed()
