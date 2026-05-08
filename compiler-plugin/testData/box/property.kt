package foo.bar

import st.evening.kt.invokecontrol.ICRestrictAnnotation
import st.evening.kt.invokecontrol.ICUnchecked

@ICRestrictAnnotation("enclave")
annotation class Enclave

fun box(): String {
    return (@ICUnchecked("enclave") Test().value)
}

class Test {
    @Enclave
    val value: String
        get() = "OK"
}
