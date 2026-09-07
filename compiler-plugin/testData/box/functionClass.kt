package foo.bar

import st.evening.kt.invokecontrol.ICRestrictAnnotation
import st.evening.kt.invokecontrol.ICUnchecked

@ICRestrictAnnotation("enclave")
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.EXPRESSION, AnnotationTarget.TYPE)
@Retention(AnnotationRetention.SOURCE)
annotation class Enclave

fun runEnclave(f: @Enclave () -> String): String {
    return (@ICUnchecked("enclave") f())
}

fun box(): String {
    val factory = Factory("OK")
    return runEnclave(factory)
}

class Factory(private val value: String) : @Enclave () -> String {
    @Enclave
    override fun invoke(): String = value
}
