package foo.bar

import st.evening.kt.invokecontrol.ICRestrict
import st.evening.kt.invokecontrol.ICRestrictAnnotation
import st.evening.kt.invokecontrol.ICUnchecked

@ICRestrictAnnotation($$"p${first}${second}n")
annotation class Enclave(val first: String, val second: String)

fun box(): String {
    return (@ICUnchecked("pinion", "perdition") foo())
}

@ICRestrict("pinion", "perdition")
fun foo(): String {
    return bar() + baz()
}

@Enclave("ini", "o")
fun bar(): String = "O"

@Enclave("erd", "itio")
fun baz(): String = "K"
