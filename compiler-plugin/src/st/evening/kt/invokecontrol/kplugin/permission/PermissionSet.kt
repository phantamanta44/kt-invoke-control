package st.evening.kt.invokecontrol.kplugin.permission

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

class PermissionSet(private val table: Map<Permission, Set<String>>) {
    companion object {
        val EMPTY: PermissionSet = PermissionSet(emptyMap())

        @OptIn(ExperimentalContracts::class)
        inline fun build(action: (Builder) -> Unit): PermissionSet {
            contract {
                callsInPlace(action, InvocationKind.EXACTLY_ONCE)
            }
            val builder = Builder()
            action(builder)
            return builder.build()
        }

        fun fromPermissions(permissions: Collection<Permission>, source: String): PermissionSet {
            if (permissions.isEmpty()) return EMPTY
            val sourceSet = setOf(source)
            return PermissionSet(permissions.associateWith { sourceSet })
        }
    }

    fun isEmpty(): Boolean = table.isEmpty()

    operator fun contains(permission: Permission): Boolean = permission in table

    fun containsAll(permissions: Collection<Permission>): Boolean = table.keys.containsAll(permissions)

    fun containsAll(o: PermissionSet): Boolean = containsAll(o.table.keys)

    fun getPermissions(): Set<Permission> = table.keys

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

    operator fun minus(o: Collection<Permission>): PermissionSet {
        val newTable = table.toMutableMap()
        o.forEach { newTable.remove(it) }
        return PermissionSet(newTable)
    }

    operator fun minus(o: PermissionSet): PermissionSet = this - o.getPermissions()

    class Builder {
        private val table: MutableMap<Permission, MutableSet<String>> = mutableMapOf()

        fun add(permission: Permission, source: String) {
            table.getOrPut(permission) { HashSet() } += source
        }

        fun addAll(permissions: Collection<Permission>, source: String) {
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
