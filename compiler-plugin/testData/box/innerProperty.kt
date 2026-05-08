package foo.bar

import st.evening.kt.invokecontrol.ICRestrictAnnotation
import st.evening.kt.invokecontrol.ICUnchecked

@ICRestrictAnnotation("enclave")
annotation class Enclave

fun box(): String {
    return (@ICUnchecked("enclave") foo())
}

@Enclave
fun foo(): String {
    val o: Test = Test("O")
    val k: Test = o.k
    return "$o$k"
}

@Enclave
class Test(val value: String) {
    val k: Test
        get() = Test("K")

    override fun toString(): String = value
}
