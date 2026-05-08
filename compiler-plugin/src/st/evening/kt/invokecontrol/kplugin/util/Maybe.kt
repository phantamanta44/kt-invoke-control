package st.evening.kt.invokecontrol.kplugin.util

sealed interface Maybe<out T> {
    object None : Maybe<Nothing>
    data class Some<T>(val value: T) : Maybe<T>
}

fun <T> Maybe<T>.orThrow(message: String): T = when (this) {
    Maybe.None -> throw IllegalStateException(message)
    is Maybe.Some<T> -> value
}

fun <T> Maybe<T>.orNull(): T? = when (this) {
    Maybe.None -> null
    is Maybe.Some<T> -> value
}

fun <T : Any> nullableToMaybe(value: T?): Maybe<T> = if (value != null) Maybe.Some(value) else Maybe.None
