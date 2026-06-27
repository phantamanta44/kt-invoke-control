package foo.bar

import st.evening.kt.invokecontrol.ICRestrict
import st.evening.kt.invokecontrol.ICUnchecked

@ICRestrict("property")
@get:ICRestrict("getter")
@set:ICRestrict("setter")
var value = "ERROR"

@ICRestrict("property", "setter")
fun doSet() {
    value = "OK"
}

fun box(): String {
    @ICUnchecked("property", "setter") doSet()
    return (@ICUnchecked("property", "getter") value)
}
