// RUN_PIPELINE_TILL: FRONTEND

package foo.bar

import st.evening.kt.invokecontrol.ICRestrictAnnotation

@ICRestrictAnnotation("enclave")
annotation class Enclave

class Box<T>(val value: T)

@Enclave
var box: Box<String> = Box("BAD")

fun update() {
    <!KIC_INSUFFICIENT_PERMISSIONS!>box<!> = Box("OK")
}

fun box(): String {
    update()
    return <!KIC_INSUFFICIENT_PERMISSIONS!>box<!>.value
}
