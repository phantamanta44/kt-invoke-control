package foo.bar

import st.evening.kt.invokecontrol.ICRestrictAnnotation
import st.evening.kt.invokecontrol.ICUnchecked

@ICRestrictAnnotation("enclave")
annotation class Enclave

fun box(): String {
    return (@ICUnchecked("enclave") Bar().talk())
}

open class Foo {
    @Enclave
    open fun talk(): String = "ER$text"

    @Enclave
    open val text: String
        get() = "ROR"
}

class Bar : Foo() {
    @Enclave
    override fun talk(): String = "O$text"

    @Enclave
    override val text: String
        get() = "K"
}
