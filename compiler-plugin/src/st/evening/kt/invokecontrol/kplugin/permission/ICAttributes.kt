package st.evening.kt.invokecontrol.kplugin.permission

import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirDeclarationDataKey
import org.jetbrains.kotlin.fir.declarations.FirDeclarationDataRegistry
import org.jetbrains.kotlin.fir.declarations.FirNamedFunction
import org.jetbrains.kotlin.fir.types.ConeAttribute
import org.jetbrains.kotlin.fir.types.ConeAttributes
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import st.evening.kt.invokecontrol.kplugin.util.Maybe
import kotlin.reflect.KClass

object ICAttributes {
    object RestrictAnnotationPermissions : FirDeclarationDataKey()
    object DeclarationPermissions : FirDeclarationDataKey()
    object SamFunctionType : FirDeclarationDataKey()
}

var FirDeclaration.icRestrictAnnotationPermissions: List<PermissionFactory>?
    by FirDeclarationDataRegistry.data(ICAttributes.RestrictAnnotationPermissions)
    internal set

var FirDeclaration.icDeclarationPermissions: Set<String>?
    by FirDeclarationDataRegistry.data(ICAttributes.DeclarationPermissions)
    internal set

var FirDeclaration.icSamFunctionType: Maybe<Pair<FirNamedFunction, ConeKotlinType>>?
    by FirDeclarationDataRegistry.data(ICAttributes.SamFunctionType)
    internal set

class FunctionTypePermissionsAttribute(
    val permissions: PermissionP
) : ConeAttribute<FunctionTypePermissionsAttribute>() {
    companion object {
        val POISON: FunctionTypePermissionsAttribute = FunctionTypePermissionsAttribute(PermissionP.Poison)
    }

    override val key: KClass<out FunctionTypePermissionsAttribute>
        get() = FunctionTypePermissionsAttribute::class
    override val keepInInferredDeclarationType: Boolean
        get() = true
    override val implementsEquality: Boolean
        get() = true

    override fun union(other: FunctionTypePermissionsAttribute?): FunctionTypePermissionsAttribute? =
        if (other == this || other == null) this else POISON

    override fun intersect(other: FunctionTypePermissionsAttribute?): FunctionTypePermissionsAttribute? =
        if (other == null) this else FunctionTypePermissionsAttribute(permissions + other.permissions)

    override fun add(other: FunctionTypePermissionsAttribute?): FunctionTypePermissionsAttribute? =
        if (other == null) this else FunctionTypePermissionsAttribute(permissions + other.permissions)

    override fun isSubtypeOf(other: FunctionTypePermissionsAttribute?): Boolean = throw UnsupportedOperationException()

    override fun toString(): String = permissions.toString()

    override fun equals(other: Any?): Boolean =
        other is FunctionTypePermissionsAttribute && permissions == other.permissions

    override fun hashCode(): Int = permissions.hashCode()
}

sealed interface PermissionP {
    companion object {
        val EMPTY: PermissionP = Some(emptySet())

        fun of(permissions: Set<String>): PermissionP = if (permissions.isEmpty()) EMPTY else Some(permissions)
    }

    operator fun plus(other: PermissionP): PermissionP

    data class Some(val permissions: Set<String>) : PermissionP {
        override fun plus(other: PermissionP): PermissionP = when (other) {
            is Some -> Some(permissions + other.permissions)
            is Poison -> Poison
        }

        override fun toString(): String = permissions.toString()
    }

    object Poison : PermissionP {
        override fun plus(other: PermissionP): PermissionP = Poison
        override fun toString(): String = "⊥"
    }
}

val ConeAttributes.icFunctionTypePermissions: FunctionTypePermissionsAttribute? by ConeAttributes.attributeAccessor()
