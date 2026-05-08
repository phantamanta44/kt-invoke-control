package foo.bar

import st.evening.kt.invokecontrol.ICRestrict
import st.evening.kt.invokecontrol.ICUnchecked

fun box(): String {
    return (@ICUnchecked("enclave") foo())
}

@ICRestrict("enclave")
fun foo(): String {
    return Test().bar()
}

@ICRestrict("enclave")
class Test {
    fun bar(): String = "OK"
}
