package foo.bar

import st.evening.kt.invokecontrol.ICConstant
import st.evening.kt.invokecontrol.ICRestrict
import st.evening.kt.invokecontrol.ICRestrictAnnotation
import st.evening.kt.invokecontrol.ICUnchecked

@ICRestrictAnnotation("enclave")
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.TYPE)
annotation class Enclave

fun runEnclave(f: @Enclave () -> String): String {
    return (@ICUnchecked("enclave") f())
}

fun runDependent(@ICConstant permission: String, f: @ICRestrict("!{permission}") () -> String): String {
    return (@ICUnchecked("!{permission}") f())
}

@Enclave
fun foo(): String = "O"

@Enclave
fun bar(): String = "K"

fun box(): String {
    return runEnclave { foo() } + runDependent("enclave") { bar() }
}
