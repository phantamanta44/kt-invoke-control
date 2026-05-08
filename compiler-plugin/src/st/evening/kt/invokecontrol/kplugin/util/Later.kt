package st.evening.kt.invokecontrol.kplugin.util

interface Later<out T> {
    val value: Maybe<T>

    class Now<T>(value: T) : Later<T> {
        override val value: Maybe.Some<T> = Maybe.Some(value)
    }

    class Indirect<T> : Later<T> {
        private var upstream: Later<T>? = null

        override val value: Maybe<T>
            get() = upstream?.value ?: Maybe.None

        fun putFrom(upstream: Later<T>) {
            if (this.upstream != null) throw IllegalStateException()
            this.upstream = upstream
        }

        fun put(value: T) {
            putFrom(Now(value))
        }
    }
}

fun <T : Any> liftNull(later: Later<T>?): Later<T?> = later ?: Later.Now(null)

fun <T> Later<T>.force(): T = value.orThrow("Later not yet resolved!")

private class BindLater<T, U>(upstream: Later<T>, transform: (T) -> Later<U>) : Later<U> {
    private sealed interface State<T, U> {
        class Initial<T, U>(val upstream: Later<T>, val transform: (T) -> Later<U>) : State<T, U>
        class Intermediate<T, U>(val upstream: Later<U>) : State<T, U>
        class Resolved<T, U>(val value: Maybe.Some<U>) : State<T, U>
    }

    private var state: State<T, U> = State.Initial(upstream, transform)

    override val value: Maybe<U>
        get() {
            when (val st = state) {
                is State.Initial<T, U> -> {
                    val value = st.upstream.value
                    if (value is Maybe.Some) {
                        val newUpstream = st.transform(value.value)
                        val finalValue = handleIntermediate(newUpstream)
                        if (finalValue is Maybe.Some<U>) return finalValue
                        state = State.Intermediate(newUpstream)
                    }
                    return Maybe.None
                }

                is State.Intermediate<T, U> -> return handleIntermediate(st.upstream)
                is State.Resolved<T, U> -> return st.value
            }
        }

    private fun handleIntermediate(upstream: Later<U>): Maybe<U> {
        val value = upstream.value
        if (value is Maybe.Some<U>) {
            state = State.Resolved(value)
            return value
        }
        return Maybe.None
    }
}

fun <T, U> Later<T>.flatMap(f: (T) -> Later<U>): Later<U> = BindLater(this, f)

fun <T, U> Later<T>.map(f: (T) -> U): Later<U> = flatMap { Later.Now(f(it)) }

private class AllLater<T>(upstreams: List<Later<T>>) : Later<List<T>> {
    private sealed interface State<T> {
        class Collecting<T>(val upstreams: List<Later<T>>) : State<T> {
            val values: MutableList<Maybe<T>> = MutableList(upstreams.size) { Maybe.None }
            var resolvedCount: Int = 0
        }

        class Resolved<T>(val values: Maybe<List<T>>) : State<T>
    }

    private var state: State<T> = State.Collecting(upstreams)

    override val value: Maybe<List<T>>
        get() {
            when (val st = state) {
                is State.Collecting<T> -> {
                    st.upstreams.forEachIndexed { index, upstream ->
                        if (st.values[index] == Maybe.None) {
                            val value = upstream.value
                            if (value is Maybe.Some<T>) {
                                st.values[index] = value
                                st.resolvedCount++
                            }
                        }
                    }
                    if (st.resolvedCount >= st.upstreams.size) {
                        val values = Maybe.Some(st.values.map { (it as Maybe.Some<T>).value })
                        state = State.Resolved(values)
                        return values
                    }
                    return Maybe.None
                }

                is State.Resolved<T> -> return st.values
            }
        }
}

fun <T> List<Later<T>>.allLater(): Later<List<T>> = AllLater(this)

fun <T, U> Collection<T>.traverseLater(f: (T) -> Later<U>): Later<List<U>> = map(f).allLater()

fun <T, U> Array<T>.traverseLater(f: (T) -> Later<U>): Later<List<U>> = map(f).allLater()

class LaterList<T>(private val upstreams: List<Later<T>>) : AbstractList<T>() {
    override val size: Int
        get() = upstreams.size

    override fun get(index: Int): T = upstreams[index].force()
}

inline fun <T, U> Collection<T>.mapToLater(f: (T) -> Later<U>): List<U> = LaterList(map(f))
