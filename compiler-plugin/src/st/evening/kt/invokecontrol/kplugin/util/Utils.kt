package st.evening.kt.invokecontrol.kplugin.util

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.evaluateAs
import org.jetbrains.kotlin.fir.declarations.findArgumentByName
import org.jetbrains.kotlin.fir.declarations.unwrapVarargValue
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.symbols.ConeTypeParameterLookupTag
import org.jetbrains.kotlin.fir.types.ConeCapturedType
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.ConeDefinitelyNotNullType
import org.jetbrains.kotlin.fir.types.ConeFlexibleType
import org.jetbrains.kotlin.fir.types.ConeIntersectionType
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.ConeKotlinTypeConflictingProjection
import org.jetbrains.kotlin.fir.types.ConeKotlinTypeProjectionIn
import org.jetbrains.kotlin.fir.types.ConeKotlinTypeProjectionOut
import org.jetbrains.kotlin.fir.types.ConeRigidType
import org.jetbrains.kotlin.fir.types.ConeSimpleKotlinType
import org.jetbrains.kotlin.fir.types.ConeStarProjection
import org.jetbrains.kotlin.fir.types.ConeTypeParameterType
import org.jetbrains.kotlin.fir.types.ConeTypeProjection
import org.jetbrains.kotlin.fir.types.ConeTypeVariableType
import org.jetbrains.kotlin.name.Name

inline fun <reified T : FirElement> FirAnnotation.forEachVarargArgument(
    name: Name,
    session: FirSession,
    action: (T) -> Unit
) {
    findArgumentByName(name)?.unwrapVarargValue()?.forEach {
        action(it.evaluateAs<T>(session)!!)
    }
}

fun ConeSimpleKotlinType.unwrapSimpleClassType(): ConeClassLikeType? = when (this) {
    is ConeCapturedType -> constructor.lowerType?.unwrapClassType()

    is ConeTypeParameterType ->
        lookupTag.typeParameterSymbol.resolvedBounds.firstNotNullOfOrNull { it.coneType.unwrapClassType() }

    is ConeTypeVariableType -> (typeConstructor.originalTypeParameter as? ConeTypeParameterLookupTag)
        ?.typeParameterSymbol?.resolvedBounds?.firstNotNullOfOrNull { it.coneType.unwrapClassType() }

    is ConeIntersectionType -> intersectedTypes.firstNotNullOfOrNull { it.unwrapClassType() } // bleh.

    is ConeClassLikeType -> this
    else -> null
}

fun ConeRigidType.unwrapRigidClassType(): ConeClassLikeType? = when (this) {
    is ConeDefinitelyNotNullType -> original.unwrapClassType()
    is ConeSimpleKotlinType -> unwrapSimpleClassType()
}

fun ConeKotlinType.unwrapClassType(): ConeClassLikeType? = when (this) {
    is ConeFlexibleType -> lowerBound.unwrapClassType()
    is ConeRigidType -> unwrapRigidClassType()
}

inline fun ConeTypeProjection.mapType(f: (ConeKotlinType) -> ConeKotlinType): ConeTypeProjection = when (this) {
    ConeStarProjection -> this
    is ConeKotlinTypeProjectionIn -> ConeKotlinTypeProjectionIn(f(type))
    is ConeKotlinTypeProjectionOut -> ConeKotlinTypeProjectionOut(f(type))
    is ConeKotlinTypeConflictingProjection -> ConeKotlinTypeConflictingProjection(f(type))
    is ConeKotlinType -> f(this)
}

inline fun ConeTypeProjection.mapTypeLater(f: (ConeKotlinType) -> Later<ConeKotlinType>): Later<ConeTypeProjection> =
    when (this) {
        ConeStarProjection -> Later.Now(this)
        is ConeKotlinTypeProjectionIn -> f(type).map { ConeKotlinTypeProjectionIn(it) }
        is ConeKotlinTypeProjectionOut -> f(type).map { ConeKotlinTypeProjectionOut(it) }
        is ConeKotlinTypeConflictingProjection -> f(type).map { ConeKotlinTypeConflictingProjection(it) }
        is ConeKotlinType -> f(this)
    }
