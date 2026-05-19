package foo.bar

import st.evening.kt.invokecontrol.ICRestrict
import st.evening.kt.invokecontrol.ICRestrictAnnotation
import st.evening.kt.invokecontrol.ICUnchecked

enum class Spin {
    UP, DOWN
}

@ICRestrictAnnotation("spin.!{value}")
annotation class Enclave(val value: Spin)

fun box(): String {
    return (@ICUnchecked("spin.UP", "spin.DOWN") foo())
}

@ICRestrict("spin.UP", "spin.DOWN")
fun foo(): String = up() + down()

@Enclave(Spin.UP)
fun up(): String = "O"

@Enclave(Spin.DOWN)
fun down(): String = "K"
