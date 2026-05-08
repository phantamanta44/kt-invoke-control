package foo.bar

import st.evening.kt.invokecontrol.ICRestrictAnnotation
import st.evening.kt.invokecontrol.ICUnchecked

@ICRestrictAnnotation("enclave")
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.EXPRESSION, AnnotationTarget.TYPE)
@Retention(AnnotationRetention.SOURCE)
annotation class Enclave // annotation restricts declarations

@Enclave
fun getSecret(): String = "OK" // function can only be accessed by other Enclave callers

@Enclave
fun doSecretWork() {
    val secret = getSecret() // call is valid because the caller is also in the Enclave
    // ...
}

fun breakIn() {
    val secret = @ICUnchecked("enclave") getSecret() // ICUnchecked is used to turn off the checker for this call
    // ...
}

inline fun <T> runInEnclave(action: @Enclave () -> T): T { // function types can also be restricted
    return (@ICUnchecked("enclave") action())
}

fun extractSecret(): String {
    return runInEnclave @Enclave { // anonymous functions can be annotated to make restricted function objects
        getSecret()
    }
}

fun box(): String = extractSecret()
