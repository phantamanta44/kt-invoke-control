package foo.bar

import st.evening.kt.invokecontrol.ICRestrictAnnotation
import st.evening.kt.invokecontrol.ICUnchecked

@ICRestrictAnnotation("enclave")
annotation class Enclave

class Box<T>(val value: T)

@Enclave
var box: Box<String> = Box("BAD")

@Enclave
fun update() {
    box = Box("OK")
}

fun box(): String {
    @ICUnchecked("enclave") update()
    return (@ICUnchecked("enclave") box).value
}
