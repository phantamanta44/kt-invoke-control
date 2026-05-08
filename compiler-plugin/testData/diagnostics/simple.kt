// RUN_PIPELINE_TILL: FRONTEND

package foo.bar

import st.evening.kt.invokecontrol.ICRestrict
import st.evening.kt.invokecontrol.ICRestrictAnnotation

@ICRestrictAnnotation("enclave")
annotation class Enclave

fun box(): String {
    return <!KIC_INSUFFICIENT_PERMISSIONS!>foo()<!>
}

@Enclave
fun foo(): String {
    return bar()
}

@ICRestrict("enclave")
fun bar(): String = "OK"
