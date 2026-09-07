// RUN_PIPELINE_TILL: FRONTEND

package foo.bar

import st.evening.kt.invokecontrol.ICRestrictAnnotation

@ICRestrictAnnotation("enclave")
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.EXPRESSION, AnnotationTarget.TYPE)
@Retention(AnnotationRetention.SOURCE)
annotation class Enclave

fun <T> runFunction(f: () -> T): T {
    return f()
}

fun test() {
    runFunction(<!KIC_LEAKY_ASSIGNMENT!>Factory("OK")<!>)
    runFunction(<!KIC_LEAKY_ASSIGNMENT!>Alerter()<!>)
}

class Factory(private val value: String) : @Enclave () -> String {
    <!KIC_LEAKY_DECLARATION!>override fun invoke(): String = value<!>
}

interface Callback : @Enclave () -> Unit

class Alerter : Callback {
    <!KIC_LEAKY_DECLARATION!>override fun invoke() {
        println("alert!")
    }<!>
}
