// RUN_PIPELINE_TILL: FRONTEND

package foo.bar

import st.evening.kt.invokecontrol.ICConstant
import st.evening.kt.invokecontrol.ICRestrict
import st.evening.kt.invokecontrol.ICRestrictAnnotation
import st.evening.kt.invokecontrol.ICUnchecked

@ICRestrictAnnotation($$"test.${param}")
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.TYPE)
@Retention(AnnotationRetention.SOURCE)
annotation class Test(val param: String)

fun grantIn(@ICConstant permission: String, action: @ICRestrict($$"${permission}") () -> Unit) {
    @ICUnchecked($$"${permission}")
    action()
}

fun grantTestIn(@ICConstant("param") permission: String, action: @Test($$"${param}") () -> Unit) {
    @ICUnchecked($$"test.${param}")
    action()
}

@ICRestrict($$"${permission}")
fun passThrough(@ICConstant permission: String, action: @ICRestrict($$"${permission}") () -> Unit) {
    action()
}

@Test($$"${s}")
fun test(@ICConstant s: String) {
    grantIn(s) @ICRestrict($$"${s}") {
        passThrough(s) @ICRestrict($$"${s}") {
            println(s)
        }
    }
}

fun nop(@ICConstant s: String) {}

const val constant: String = "lorem"
val property: String = "ipsum"
fun function(): String = "dolor"

fun main(@ICConstant constantParameter: String, parameter: String) {
    grantIn(constant) @ICRestrict("lorem") {}
    nop(<!KIC_NOT_CONSTANT!>property<!>)
    nop(<!KIC_NOT_CONSTANT!>function()<!>)
    grantIn(constantParameter) @ICRestrict($$"${constantParameter}") {}
    nop(<!KIC_NOT_CONSTANT!>parameter<!>)
    grantIn("literal") @ICRestrict("literal") {}
    nop(<!KIC_NO_SUCH_PERMISSION_ARGUMENT!>$$"${nothing} to see here"<!>)

    grantTestIn("foo") @ICRestrict("test.foo") {
        test("foo")
    }
}

@ICRestrict(<!KIC_NO_SUCH_PERMISSION_ARGUMENT!>$$"${foo}"<!>)
fun bad(@ICConstant("bar") foo: String) {}
