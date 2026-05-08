// RUN_PIPELINE_TILL: FRONTEND

package foo.bar

import st.evening.kt.invokecontrol.ICRestrict

fun box(): String {
    return Foo2().test() + Bar2().test()
}

@ICRestrict("enclave")
open class Foo1 {
    open fun test(): String = "Z"
}

<!KIC_LEAKY_DECLARATION!>class Foo2 : <!KIC_INSUFFICIENT_PERMISSIONS!>Foo1()<!> {
    override fun test(): String = "O"
}<!>

@ICRestrict("enclave")
interface Bar1 {
    fun test(): String
}

<!KIC_LEAKY_DECLARATION!>class Bar2 : Bar1 {
    override fun test(): String = "K"
}<!>
