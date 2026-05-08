package st.evening.kt.invokecontrol.kplugin.util

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.SessionAndScopeSessionHolder
import org.jetbrains.kotlin.fir.SessionHolder
import org.jetbrains.kotlin.fir.declarations.FirNamedFunction
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.FirTypeParameterRefsOwner
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.ConeKotlinType

private val cFirSamResolverKt = Class.forName("org.jetbrains.kotlin.fir.resolve.FirSamResolverKt")

private val mFirSamResolverKt_getSingleAbstractMethodOrNull = cFirSamResolverKt.getDeclaredMethod(
    "getSingleAbstractMethodOrNull",
    SessionAndScopeSessionHolder::class.java,
    FirRegularClass::class.java
).apply { isAccessible = true }

context(c: SessionAndScopeSessionHolder)
fun FirRegularClass.getSingleAbstractMethodOrNull(): FirNamedFunction? =
    mFirSamResolverKt_getSingleAbstractMethodOrNull(null, c, this) as FirNamedFunction?

private val mFirSamResolverKt_getFunctionTypeForAbstractMethod = cFirSamResolverKt.getDeclaredMethod(
    "getFunctionTypeForAbstractMethod",
    FirNamedFunction::class.java,
    FirSession::class.java
).apply { isAccessible = true }

fun FirNamedFunction.getFunctionTypeForAbstractMethod(session: FirSession): ConeKotlinType =
    mFirSamResolverKt_getFunctionTypeForAbstractMethod(null, this, session) as ConeKotlinType

private val mFirSamResolverKt_buildSubstitutorWithUpperBounds = cFirSamResolverKt.getDeclaredMethod(
    "buildSubstitutorWithUpperBounds",
    SessionHolder::class.java,
    FirTypeParameterRefsOwner::class.java,
    ConeClassLikeType::class.java
).apply { isAccessible = true }

context(c: SessionHolder)
fun FirTypeParameterRefsOwner.buildSubstitutorWithUpperBounds(type: ConeClassLikeType): ConeSubstitutor =
    mFirSamResolverKt_buildSubstitutorWithUpperBounds(null, c, this, type) as ConeSubstitutor
