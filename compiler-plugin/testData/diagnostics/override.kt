// RUN_PIPELINE_TILL: FRONTEND

package foo.bar

import st.evening.kt.invokecontrol.ICRestrictAnnotation

@ICRestrictAnnotation("enclave")
annotation class Enclave

fun box(): String {
    return Bar().talk()
}

open class Foo {
    @Enclave
    open fun talk(): String = "ER$text"

    @Enclave
    open val text: String
        get() = "ROR"
}

class Bar : Foo() {
    <!KIC_LEAKY_DECLARATION!>override fun talk(): String = "O$text"<!>

    <!KIC_LEAKY_DECLARATION!>override val text: String
        get() = "K"<!>
}
