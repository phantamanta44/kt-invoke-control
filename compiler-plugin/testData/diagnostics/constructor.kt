// RUN_PIPELINE_TILL: FRONTEND

package foo.bar

import st.evening.kt.invokecontrol.ICRestrict

fun box(): String {
    return foo()
}

fun foo(): String {
    return <!KIC_INSUFFICIENT_PERMISSIONS!>Test()<!>.bar()
}

@ICRestrict("enclave")
class Test {
    fun bar(): String = "OK"
}
