package foo.bar

import st.evening.kt.invokecontrol.ICRestrictAnnotation
import st.evening.kt.invokecontrol.ICUnchecked

@ICRestrictAnnotation("enclave")
annotation class Enclave

fun box(): String {
    return (@ICUnchecked("enclave") Test()).value
}

class Test @Enclave private constructor(val value: String) {
    @Enclave
    constructor() : this("OK")
}
