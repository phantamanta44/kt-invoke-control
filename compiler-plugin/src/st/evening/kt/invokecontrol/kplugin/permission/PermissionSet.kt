package st.evening.kt.invokecontrol.kplugin.permission


class PermissionSet(private val table: Map<String, Set<String>>) {
    companion object {
        val EMPTY: PermissionSet = PermissionSet(emptyMap())

        inline fun build(action: (Builder) -> Unit): PermissionSet {
            val builder = Builder()
            action(builder)
            return builder.build()
        }
    }

    fun isEmpty(): Boolean = table.isEmpty()

    operator fun contains(permission: String): Boolean = permission in table

    fun containsAll(permissions: Collection<String>): Boolean = table.keys.containsAll(permissions)

    fun containsAll(o: PermissionSet): Boolean = containsAll(o.table.keys)

    fun getPermissions(): Set<String> = table.keys

    fun getProvenance(): Set<String> = table.values.flatMapTo(mutableSetOf()) { it }

    operator fun plus(o: PermissionSet): PermissionSet {
        val result = table.toMutableMap()
        o.table.forEach { (permission, sources) ->
            when (val existingSources = result[permission]) {
                null -> result[permission] = HashSet()
                is HashSet<String> -> existingSources.addAll(sources)
                else -> result[permission] = HashSet(existingSources).apply { addAll(sources) }
            }
        }
        return PermissionSet(result)
    }

    operator fun minus(o: Collection<String>): PermissionSet {
        val newTable = table.toMutableMap()
        o.forEach { newTable.remove(it) }
        return PermissionSet(newTable)
    }

    operator fun minus(o: PermissionSet): PermissionSet = this - o.getPermissions()

    class Builder {
        private val table: MutableMap<String, MutableSet<String>> = mutableMapOf()

        fun add(permission: String, source: String) {
            table.getOrPut(permission) { HashSet() } += source
        }

        fun addAll(permissions: Collection<String>, source: String) {
            permissions.forEach { add(it, source) }
        }

        fun addAll(permissions: PermissionSet) {
            permissions.table.forEach { (permission, sources) ->
                table.getOrPut(permission) { HashSet() }.addAll(sources)
            }
        }

        fun build(): PermissionSet = if (table.isEmpty()) EMPTY else PermissionSet(table)
    }
}
