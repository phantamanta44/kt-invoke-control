package st.evening.kt.invokecontrol.kplugin.permission

import org.jetbrains.kotlin.diagnostics.DiagnosticContext
import org.jetbrains.kotlin.fir.types.ConeAttribute
import org.jetbrains.kotlin.fir.types.ConeAttributes
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.ConeTypeContext
import org.jetbrains.kotlin.fir.types.withAttributes
import st.evening.kt.invokecontrol.kplugin.util.Later
import st.evening.kt.invokecontrol.kplugin.util.LeafTypeTransformState
import st.evening.kt.invokecontrol.kplugin.util.force
import st.evening.kt.invokecontrol.kplugin.util.transformLeaves

context(_: DiagnosticContext)
fun Permission.substituteOrSelf(substitution: Permission.Substitution?): Permission =
    substitution?.let { substitute(it) } ?: this

context(_: DiagnosticContext)
fun Set<Permission>.substitute(substitution: Permission.Substitution): Set<Permission> =
    mapNotNullTo(mutableSetOf()) { it.substitute(substitution) }

context(_: DiagnosticContext)
fun Set<Permission>.substituteOrSelf(substitution: Permission.Substitution?): Set<Permission> =
    substitution?.let { substitute(it) } ?: this

context(_: DiagnosticContext, _: ConeTypeContext)
fun ConeKotlinType.substitute(substitution: Permission.Substitution): ConeKotlinType =
    transformLeaves(LeafTypeTransformState()) { type ->
        val attributes = type.attributes
        val attribute = attributes.icFunctionTypePermissions ?: return@transformLeaves Later.Now(type)
        when (val permissions = attribute.permissions) {
            PermissionP.Poison -> return@transformLeaves Later.Now(type)
            is PermissionP.Some -> {
                val newAttributes = mutableListOf<ConeAttribute<*>>()
                attributes.forEach {
                    if (it.key == FunctionTypePermissionsAttribute::class) {
                        newAttributes += FunctionTypePermissionsAttribute(
                            PermissionP.Some(permissions.permissions.substitute(substitution))
                        )
                    } else {
                        newAttributes += it
                    }
                }
                return@transformLeaves Later.Now(type.withAttributes(ConeAttributes.create(newAttributes)))
            }
        }
    }.force()

context(_: DiagnosticContext, _: ConeTypeContext)
fun ConeKotlinType.substituteOrSelf(substitution: Permission.Substitution?): ConeKotlinType =
    substitution?.let { substitute(it) } ?: this
