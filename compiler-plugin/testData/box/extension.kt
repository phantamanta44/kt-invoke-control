package foo.bar

import kotlin.reflect.KType
import kotlin.reflect.typeOf
import st.evening.kt.invokecontrol.ICRestrictAnnotation
import st.evening.kt.invokecontrol.ICUnchecked

@ICRestrictAnnotation("enclave")
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.TYPE, AnnotationTarget.EXPRESSION)
@Retention(AnnotationRetention.SOURCE)
annotation class Enclave

fun box(): String {
    val f: @Enclave (String) -> String = { it + "O" }
    val g = f.update {
        @Enclave { s -> flip(it(s)) }
    }
    return (@ICUnchecked("enclave") g("K"))
}

inline fun <T> T.update(op: (T) -> T): T = op(this)

@Enclave
fun flip(s: String): String = s.reversed()
