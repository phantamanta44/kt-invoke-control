package foo.bar

import st.evening.kt.invokecontrol.ICRestrictAnnotation
import st.evening.kt.invokecontrol.ICUnchecked

@ICRestrictAnnotation("enclave")
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.EXPRESSION, AnnotationTarget.TYPE)
@Retention(AnnotationRetention.SOURCE)
annotation class Enclave

fun box(): String {
    return runEnclave @Enclave {
        val x = fromAlphabet { theLetter(it, 14) }
        val y = fromAlphabet { theLetter(it, 10) }
        return@runEnclave "$x$y"
    }
}

fun <T> runEnclave(f: @Enclave () -> T): T {
    return (@ICUnchecked("enclave") f())
}

fun <T> fromAlphabet(f: (String) -> T): T {
    return f("ABCDEFGHIJKLMNOPQRSTUVWXYZ")
}

@Enclave
fun theLetter(alphabet: String, index: Int): Char = alphabet[index]
