// RUN_PIPELINE_TILL: FRONTEND

package foo.bar

import st.evening.kt.invokecontrol.ICRestrictAnnotation
import st.evening.kt.invokecontrol.ICUnchecked

@ICRestrictAnnotation("enclave")
annotation class Enclave

@Enclave
object Utils {
    fun ok(): String = "OK"
}

fun box(): String {
    return (@ICUnchecked("enclave") Utils).ok()
}
