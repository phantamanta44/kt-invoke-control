// RUN_PIPELINE_TILL: FRONTEND

package foo.bar

import st.evening.kt.invokecontrol.ICRestrictAnnotation

@ICRestrictAnnotation("enclave")
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.EXPRESSION, AnnotationTarget.TYPE)
@Retention(AnnotationRetention.SOURCE)
annotation class Enclave

fun box(): String {
    return runFactory @Enclave <!KIC_LEAKY_ASSIGNMENT!>{ okay() }<!>
}

fun interface OkFactory {
    fun cookOneUp(): String
}

fun runFactory(factory: OkFactory): String {
    return factory.cookOneUp()
}

@Enclave
fun okay(): String = "OK"
