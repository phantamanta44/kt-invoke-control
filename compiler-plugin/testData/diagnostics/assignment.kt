// RUN_PIPELINE_TILL: FRONTEND

package foo.bar

import st.evening.kt.invokecontrol.ICRestrictAnnotation

@ICRestrictAnnotation("enclave")
@Target(AnnotationTarget.TYPE)
@Retention(AnnotationRetention.SOURCE)
annotation class R

open class A
open class B : A()

val f: (A) -> B = throw NotImplementedError()
val g: @R (A) -> B = throw NotImplementedError()

val h: (A) -> @R (B) -> B = throw NotImplementedError()
val i: (@R (A) -> B) -> B = throw NotImplementedError()

val j: Comparable<@R (A) -> B> = throw NotImplementedError()
val k: List<@R (A) -> B> = throw NotImplementedError()

fun test() {
    val fgTest1: (A) -> B = f
    val fgTest2: @R (A) -> B = f
    val fgTest3: (A) -> B = <!KIC_LEAKY_ASSIGNMENT!>g<!>
    val fgTest4: @R (A) -> B = g
    val fgTest5: (A) -> A = f
    val fgTest6: (A) -> A = <!KIC_LEAKY_ASSIGNMENT!>g<!>
    val fgTest7: @R (A) -> A = f
    val fgTest8: @R (A) -> A = g

    val hiTest1: (A) -> (B) -> B = <!KIC_LEAKY_ASSIGNMENT!>h<!>
    val hiTest2: ((A) -> B) -> B = i
    val hiTest3: @R (A) -> (B) -> B = <!KIC_LEAKY_ASSIGNMENT!>h<!>
    val hiTest4: @R ((A) -> B) -> B = i
    val hiTest5: (A) -> @R (B) -> B = h
    val hiTest6: (@R (A) -> B) -> B = i
    val hiTest7: @R (A) -> @R (B) -> B = h
    val hiTest8: @R (@R (A) -> B) -> B = i

    val jkTest1: Int = j.compareTo(f)
    val jkTest2: (A) -> B = <!KIC_LEAKY_ASSIGNMENT!>k.get(0)<!>
    val jkTest3: Int = j.compareTo(g)
    val jkTest4: @R (A) -> B = k.get(0)
}
