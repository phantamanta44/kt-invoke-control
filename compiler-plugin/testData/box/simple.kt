package foo.bar

import st.evening.kt.invokecontrol.ICRestrict
import st.evening.kt.invokecontrol.ICRestrictAnnotation
import st.evening.kt.invokecontrol.ICUnchecked

@ICRestrictAnnotation("enclave")
annotation class Enclave

fun box(): String {
    return (@ICUnchecked("enclave") foo())
}

@Enclave
fun foo(): String {
    return bar()
}

@ICRestrict("enclave")
fun bar(): String = "OK"
