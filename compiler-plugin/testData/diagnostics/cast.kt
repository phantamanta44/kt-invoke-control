// RUN_PIPELINE_TILL: BACKEND

package foo.bar

import st.evening.kt.invokecontrol.ICRestrictAnnotation

@ICRestrictAnnotation("enclave")
@Target(AnnotationTarget.TYPE)
@Retention(AnnotationRetention.SOURCE)
annotation class R

@ICRestrictAnnotation("secret")
@Target(AnnotationTarget.TYPE)
@Retention(AnnotationRetention.SOURCE)
annotation class X

val f: () -> @R () -> String = throw NotImplementedError()

@Suppress("USELESS_CAST")
fun box() {
    val test1 = <!KIC_LEAKY_CAST!>f as (() -> () -> String)<!>
    val test2 = f as (@R () -> @R () -> String)
    val test3 = <!KIC_LEAKY_CAST!>f as (() -> @X () -> String)<!>
}
