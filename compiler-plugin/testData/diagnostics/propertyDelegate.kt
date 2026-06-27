// RUN_PIPELINE_TILL: FRONTEND

package foo.bar

import st.evening.kt.invokecontrol.ICRestrictAnnotation

@ICRestrictAnnotation("enclave")
annotation class Enclave

@Enclave
fun ok(): Lazy<String> = lazy { "OK" }

val value: String by <!KIC_INSUFFICIENT_PERMISSIONS!>ok()<!>

fun box(): String {
    return value
}
