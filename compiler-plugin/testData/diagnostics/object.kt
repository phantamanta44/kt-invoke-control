// RUN_PIPELINE_TILL: FRONTEND

package foo.bar

import st.evening.kt.invokecontrol.ICRestrictAnnotation

@ICRestrictAnnotation("enclave")
annotation class Enclave

@Enclave
object Utils {
    fun ok(): String = "OK"
}

fun box(): String {
    return <!KIC_INSUFFICIENT_PERMISSIONS!>Utils<!>.ok()
}
