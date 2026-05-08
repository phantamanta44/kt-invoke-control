// RUN_PIPELINE_TILL: FRONTEND

package foo.bar

import st.evening.kt.invokecontrol.ICRestrictAnnotation

@ICRestrictAnnotation("enclave")
@Target(AnnotationTarget.TYPE)
@Retention(AnnotationRetention.SOURCE)
annotation class Enclave

val foo: @Enclave (String) -> String = throw NotImplementedError()

fun bar(): @Enclave (String) -> String = throw NotImplementedError()

fun test() {
    runF(<!KIC_LEAKY_ASSIGNMENT!>foo<!>)
    runF(<!KIC_LEAKY_ASSIGNMENT!>bar()<!>)
    runT(<!KIC_LEAKY_ASSIGNMENT!>foo<!>)
    runT(<!KIC_LEAKY_ASSIGNMENT!>bar()<!>)
}

fun runF(f: (String) -> String): String = f("test")

fun runT(transformer: StringTransformer): String = transformer.apply("test")

fun interface StringTransformer {
    fun apply(value: String): String
}
