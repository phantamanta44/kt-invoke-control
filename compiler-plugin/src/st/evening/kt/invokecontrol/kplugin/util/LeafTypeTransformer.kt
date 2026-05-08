package st.evening.kt.invokecontrol.kplugin.util

import org.jetbrains.kotlin.fir.types.ConeCapturedType
import org.jetbrains.kotlin.fir.types.ConeCapturedTypeConstructor
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.ConeDefinitelyNotNullType
import org.jetbrains.kotlin.fir.types.ConeDynamicType
import org.jetbrains.kotlin.fir.types.ConeFlexibleType
import org.jetbrains.kotlin.fir.types.ConeIntegerLiteralType
import org.jetbrains.kotlin.fir.types.ConeIntersectionType
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.ConeLookupTagBasedType
import org.jetbrains.kotlin.fir.types.ConeRawType
import org.jetbrains.kotlin.fir.types.ConeRigidType
import org.jetbrains.kotlin.fir.types.ConeSimpleKotlinType
import org.jetbrains.kotlin.fir.types.ConeStubType
import org.jetbrains.kotlin.fir.types.ConeTypeContext
import org.jetbrains.kotlin.fir.types.ConeTypeParameterType
import org.jetbrains.kotlin.fir.types.ConeTypeVariableType
import org.jetbrains.kotlin.fir.types.toTrivialFlexibleType

// laziness allows transformation of self-referential types, particularly through `LaterList`s as in the captured case
typealias LeafTypeTransformer = (ConeSimpleKotlinType) -> Later<ConeSimpleKotlinType>

class LeafTypeTransformState {
    private val cache: MutableMap<ConeKotlinType, Later<ConeKotlinType>> = mutableMapOf()

    @Suppress("UNCHECKED_CAST")
    fun <T : ConeKotlinType> getCacheEntry(type: T): Later<T>? = cache[type]?.let { it as Later<T> }

    fun <T : ConeKotlinType> createCacheEntry(type: T): Later.Indirect<T> =
        Later.Indirect<T>().also { cache[type] = it }

    inline fun <T : ConeKotlinType> withCache(type: T, transform: () -> Later<T>): Later<T> {
        getCacheEntry(type)?.let {
            return it
        }
        val entry = createCacheEntry(type)
        val transformed = transform()
        entry.putFrom(transformed)
        return entry
    }
}

context(typeContext: ConeTypeContext)
fun ConeFlexibleType.transformLeavesFlexible(
    state: LeafTypeTransformState,
    f: LeafTypeTransformer
): Later<ConeFlexibleType> = when (this) {
    is ConeDynamicType -> Later.Now(this)

    is ConeRawType -> state.withCache(this) {
        lowerBound.transformLeavesRigid(state, f).flatMap { lower ->
            upperBound.transformLeavesRigid(state, f).map { upper ->
                ConeRawType.create(lower, upper)
            }
        }
    }

    else -> state.withCache(this) {
        if (isTrivial) {
            lowerBound.transformLeavesRigid(state, f).map { it.toTrivialFlexibleType(typeContext) }
        } else {
            lowerBound.transformLeavesRigid(state, f).flatMap { lower ->
                upperBound.transformLeavesRigid(state, f).map { upper ->
                    ConeFlexibleType(lower, upper, false)
                }
            }
        }
    }
}

fun ConeLookupTagBasedType.transformLeavesLookup(
    state: LeafTypeTransformState,
    f: LeafTypeTransformer
): Later<ConeSimpleKotlinType> = when (this) {
    is ConeClassLikeType -> state.withCache(this) { f(this) }
    is ConeTypeParameterType -> state.withCache(this) { f(this) }
    else -> throw UnsupportedOperationException(javaClass.canonicalName)
}

context(typeContext: ConeTypeContext)
fun ConeSimpleKotlinType.transformLeavesSimple(
    state: LeafTypeTransformState,
    f: LeafTypeTransformer
): Later<ConeSimpleKotlinType> = when (this) {
    is ConeCapturedType -> state.withCache(this) {
        constructor.let { ctor ->
            ctor.projection.mapTypeLater { it.transformLeaves(state, f) }.flatMap { projection ->
                liftNull(ctor.lowerType?.transformLeaves(state, f)).map { lower ->
                    ConeCapturedType(
                        isMarkedNullable,
                        ConeCapturedTypeConstructor(
                            projection,
                            lower,
                            ctor.captureStatus,
                            // memoization + laziness prevents infinite loops in cyclical types here
                            ctor.supertypes?.mapToLater { it.transformLeaves(state, f) },
                            ctor.typeParameterMarker
                        ),
                        attributes
                    )
                }
            }
        }
    }

    is ConeIntersectionType -> state.withCache(this) {
        liftNull(upperBoundForApproximation?.transformLeaves(state, f)).map { upper ->
            ConeIntersectionType(intersectedTypes.mapToLater { it.transformLeaves(state, f) }, upper)
        }
    }

    is ConeIntegerLiteralType -> Later.Now(this)
    is ConeStubType -> Later.Now(this)
    is ConeLookupTagBasedType -> transformLeavesLookup(state, f)
    is ConeTypeVariableType -> state.withCache(this) { f(this) }
}

context(typeContext: ConeTypeContext)
fun ConeRigidType.transformLeavesRigid(
    state: LeafTypeTransformState,
    f: LeafTypeTransformer
): Later<ConeRigidType> = when (this) {
    is ConeDefinitelyNotNullType -> state.withCache(this) {
        original.transformLeavesSimple(state, f).map { ConeDefinitelyNotNullType(it) }
    }

    is ConeSimpleKotlinType -> transformLeavesSimple(state, f)
}

context(typeContext: ConeTypeContext)
fun ConeKotlinType.transformLeaves(
    state: LeafTypeTransformState,
    f: LeafTypeTransformer
): Later<ConeKotlinType> = when (this) {
    is ConeFlexibleType -> transformLeavesFlexible(state, f)
    is ConeRigidType -> transformLeavesRigid(state, f)
}
