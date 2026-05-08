// RUN_PIPELINE_TILL: FRONTEND

package foo.bar

import st.evening.kt.invokecontrol.ICRestrictAnnotation

@ICRestrictAnnotation("enclave")
annotation class Enclave

fun box(): String {
    return Test().value
}

class Test @Enclave private constructor(val value: String) {
    constructor() : <!KIC_INSUFFICIENT_PERMISSIONS!>this<!>("OK")
}
