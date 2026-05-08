// RUN_PIPELINE_TILL: FRONTEND

package foo.bar

import st.evening.kt.invokecontrol.ICRestrictAnnotation

@ICRestrictAnnotation("enclave")
annotation class Enclave

fun box(): String {
    return <!KIC_INSUFFICIENT_PERMISSIONS!>Test().value<!>
}

class Test {
    @Enclave
    val value: String
        get() = "OK"
}
