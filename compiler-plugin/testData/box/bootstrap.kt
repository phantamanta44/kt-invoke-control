package foo.bar

class SomeEnumValue<E : Enum<E>>(val value: E)

fun <E : Enum<E>> extract(e: SomeEnumValue<E>): String = e.value.name

inline fun <T> maxBy(elements: List<T>, comparator: (T, T) -> Int): T {
    var best = elements[0]
    for (i in 1..<elements.size) {
        val here = elements[i]
        if (comparator(here, best) > 0) {
            best = here
        }
    }
    return best
}

fun <T : Comparable<T>> max(elements: List<T>): T {
    return maxBy(elements) { x, y -> x.compareTo(y) }
}

fun box(): String {
    return max(listOf("BAD", "ERROR", "OK", "FAIL"))
}
