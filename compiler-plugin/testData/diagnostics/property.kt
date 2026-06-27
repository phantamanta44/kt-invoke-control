// RUN_PIPELINE_TILL: FRONTEND

package foo.bar

import st.evening.kt.invokecontrol.ICRestrictAnnotation

@ICRestrictAnnotation("enclave")
annotation class Enclave

fun box(): String {
    val x = Test()
    <!KIC_INSUFFICIENT_PERMISSIONS!>x.variable<!> = "OK"
    return <!KIC_INSUFFICIENT_PERMISSIONS!>x.value<!>
}

class Test {
    @Enclave
    var variable: String = "ERROR"

    @Enclave
    val value: String
        get() = variable
}
