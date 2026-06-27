package foo.bar

import st.evening.kt.invokecontrol.ICRestrictAnnotation
import st.evening.kt.invokecontrol.ICUnchecked

@ICRestrictAnnotation("enclave")
annotation class Enclave

@Enclave
fun ok(): Lazy<String> = lazy { "OK" }

@Enclave
val value: String by ok()

fun box(): String {
    return (@ICUnchecked("enclave") value)
}
