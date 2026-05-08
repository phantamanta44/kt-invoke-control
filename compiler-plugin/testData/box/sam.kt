package foo.bar

import st.evening.kt.invokecontrol.ICRestrictAnnotation
import st.evening.kt.invokecontrol.ICUnchecked

@ICRestrictAnnotation("enclave")
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.EXPRESSION, AnnotationTarget.TYPE)
@Retention(AnnotationRetention.SOURCE)
annotation class Enclave

fun box(): String {
    return runFactory @Enclave { okay() }
}

fun interface OkFactory {
    @Enclave
    fun cookOneUp(): String
}

fun runFactory(factory: OkFactory): String {
    return (@ICUnchecked("enclave") factory.cookOneUp())
}

@Enclave
fun okay(): String = "OK"
