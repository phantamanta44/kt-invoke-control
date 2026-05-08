// RUN_PIPELINE_TILL: BACKEND

package foo.bar

import st.evening.kt.invokecontrol.ICRestrict
import st.evening.kt.invokecontrol.ICRestrictAnnotation
import st.evening.kt.invokecontrol.ICUnchecked

@ICRestrictAnnotation("enclave")
annotation class Enclave

<!KIC_EMPTY_ANNOTATION!>@ICRestrictAnnotation()<!>
annotation class Nonclave

fun box(): String {
    return (<!KIC_REDUNDANT_UNCHECKED!>@ICUnchecked("enclave", "unused")<!> foo()) + (<!KIC_EMPTY_ANNOTATION!>@ICUnchecked()<!> bar())
}

@Enclave
fun foo(): String {
    return "O"
}

@Nonclave
<!KIC_EMPTY_ANNOTATION!>@ICRestrict()<!>
fun bar(): String {
    return "K"
}
