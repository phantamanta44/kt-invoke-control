package foo.bar

import st.evening.kt.invokecontrol.ICRestrictAnnotation
import st.evening.kt.invokecontrol.ICUnchecked

@ICRestrictAnnotation("enclave")
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY, AnnotationTarget.EXPRESSION, AnnotationTarget.TYPE)
@Retention(AnnotationRetention.SOURCE)
annotation class Enclave

fun box(): String {
    return runEnclave(::foo) + runEnclave(::bar)
}

fun <T> runEnclave(f: @Enclave () -> T): T {
    return (@ICUnchecked("enclave") f())
}

@Enclave
fun foo(): String = "O"

@Enclave
val bar: String
    get() = "K"
