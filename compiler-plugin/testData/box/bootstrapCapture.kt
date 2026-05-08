package foo.bar

class SomeEnumValue<E : Enum<E>>(val value: E)

fun <E : Enum<E>> extract(e: SomeEnumValue<E>): String = e.value.name

enum class Status { OK }

val ok: Any = SomeEnumValue(Status.OK)

fun box(): String {
    return if (ok is SomeEnumValue<*>) extract(ok) else "FAIL"
}
