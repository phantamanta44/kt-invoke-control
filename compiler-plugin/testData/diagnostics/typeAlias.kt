// RUN_PIPELINE_TILL: FRONTEND
package foo.bar

import st.evening.kt.invokecontrol.ICRestrictAnnotation

@ICRestrictAnnotation("enclave")
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.EXPRESSION, AnnotationTarget.TYPE)
@Retention(AnnotationRetention.SOURCE)
annotation class Enclave

typealias Name = String

val test1: Name = "world"

val test2: Name
    get() = "universe"

var test3: Name = "Constantinople"

lateinit var test4: Name

fun greet(name: Name) {
    println("Hello, $name!")
}

typealias Two<T> = Pair<T, T>

val test5: Two<String> = "oath" to "order"

val test6: Two<Int>
    get() = 9 to 5

var test7: Two<String> = "north" to "south"

lateinit var test8: Two<Int>

fun printFirst(two: Two<*>) {
    println(two.first)
}

typealias Op<T> = @Enclave (T) -> T

val test9: Op<Int> = { -it }

val test10: Op<String>
    get() = { it + it }

var test11: Op<Int> = { it + 1 }

lateinit var test12: Op<String>

fun box(): String {
    greet(test1)
    greet(test2)
    test3 = "Istanbul"
    greet(test3)
    test4 = "Amy"
    greet(test4)

    printFirst(test5)
    printFirst(test6)
    test7 = "east" to "west"
    printFirst(test7)
    test8 = 1 to 9
    printFirst(test8)

    println(<!KIC_INSUFFICIENT_PERMISSIONS!>test9(42)<!>)
    println(<!KIC_INSUFFICIENT_PERMISSIONS!>test10("foot")<!>)
    test11 = { it - 1 }
    println(<!KIC_INSUFFICIENT_PERMISSIONS!>test11(42)<!>)
    test12 = { it.uppercase() }
    return <!KIC_INSUFFICIENT_PERMISSIONS!>test12("ok")<!>
}
