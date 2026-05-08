package st.evening.kt.invokecontrol.kplugin.resolve

import org.jetbrains.kotlin.AbstractKtSourceElement
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.diagnostics.DiagnosticContext
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.KtDiagnostic
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.SessionAndScopeSessionHolder
import org.jetbrains.kotlin.fir.declarations.FirAnonymousFunction
import org.jetbrains.kotlin.fir.declarations.FirCallableDeclaration
import org.jetbrains.kotlin.fir.declarations.FirConstructor
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirNamedFunction
import org.jetbrains.kotlin.fir.declarations.FirReceiverParameter
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.evaluateAs
import org.jetbrains.kotlin.fir.declarations.extractEnumValueArgumentInfo
import org.jetbrains.kotlin.fir.declarations.findArgumentByName
import org.jetbrains.kotlin.fir.declarations.toAnnotationClassIdSafe
import org.jetbrains.kotlin.fir.declarations.toAnnotationClassLikeSymbol
import org.jetbrains.kotlin.fir.declarations.utils.classId
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirLiteralExpression
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.expressions.createConeSubstitutorFromTypeArguments
import org.jetbrains.kotlin.fir.languageVersionSettings
import org.jetbrains.kotlin.fir.references.FirNamedReference
import org.jetbrains.kotlin.fir.references.resolved
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.resolve.toClassLikeSymbol
import org.jetbrains.kotlin.fir.resolve.toRegularClassSymbol
import org.jetbrains.kotlin.fir.session.sourcesToPathsMapper
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.types.ConeCapturedType
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.ConeFlexibleType
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.ConeTypeProjection
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRefCopy
import org.jetbrains.kotlin.fir.types.impl.ConeClassLikeTypeImpl
import org.jetbrains.kotlin.fir.types.isSomeFunctionType
import org.jetbrains.kotlin.fir.types.lowerBoundIfFlexible
import org.jetbrains.kotlin.fir.types.renderReadable
import org.jetbrains.kotlin.fir.types.resolvedType
import org.jetbrains.kotlin.fir.types.toTrivialFlexibleType
import org.jetbrains.kotlin.fir.types.typeAnnotations
import org.jetbrains.kotlin.fir.types.typeContext
import org.jetbrains.kotlin.fir.types.upperBoundIfFlexible
import org.jetbrains.kotlin.fir.types.withAttributes
import org.jetbrains.kotlin.fir.types.withNullabilityOf
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.util.wrapIntoFileAnalysisExceptionIfNeeded
import st.evening.kt.invokecontrol.kplugin.ICCommandLineProcessor
import st.evening.kt.invokecontrol.kplugin.ICDiagnostics
import st.evening.kt.invokecontrol.kplugin.ICNames
import st.evening.kt.invokecontrol.kplugin.permission.FunctionTypePermissionsAttribute
import st.evening.kt.invokecontrol.kplugin.permission.PermissionArgumentProvider
import st.evening.kt.invokecontrol.kplugin.permission.PermissionFactory
import st.evening.kt.invokecontrol.kplugin.permission.PermissionP
import st.evening.kt.invokecontrol.kplugin.permission.icDeclarationPermissions
import st.evening.kt.invokecontrol.kplugin.permission.icFunctionTypePermissions
import st.evening.kt.invokecontrol.kplugin.permission.icRestrictAnnotationPermissions
import st.evening.kt.invokecontrol.kplugin.permission.icSamFunctionType
import st.evening.kt.invokecontrol.kplugin.util.Later
import st.evening.kt.invokecontrol.kplugin.util.LeafTypeTransformState
import st.evening.kt.invokecontrol.kplugin.util.buildSubstitutorWithUpperBounds
import st.evening.kt.invokecontrol.kplugin.util.forEachVarargArgument
import st.evening.kt.invokecontrol.kplugin.util.force
import st.evening.kt.invokecontrol.kplugin.util.getFunctionTypeForAbstractMethod
import st.evening.kt.invokecontrol.kplugin.util.getSingleAbstractMethodOrNull
import st.evening.kt.invokecontrol.kplugin.util.map
import st.evening.kt.invokecontrol.kplugin.util.mapType
import st.evening.kt.invokecontrol.kplugin.util.mapTypeLater
import st.evening.kt.invokecontrol.kplugin.util.nullableToMaybe
import st.evening.kt.invokecontrol.kplugin.util.orNull
import st.evening.kt.invokecontrol.kplugin.util.transformLeaves
import st.evening.kt.invokecontrol.kplugin.util.traverseLater

class ICResolveService(
    override val session: FirSession,
    private val compilerConfig: CompilerConfiguration
) : SessionAndScopeSessionHolder {
    private inner class ResolveContext(override val containingFilePath: String?) : DiagnosticContext {
        constructor(file: FirFile) : this(file.source?.let { session.sourcesToPathsMapper.getSourceFilePath(it) })

        override val languageVersionSettings: LanguageVersionSettings
            get() = session.languageVersionSettings

        override fun isDiagnosticSuppressed(diagnostic: KtDiagnostic): Boolean = false
    }

    // Phase 1: resolve restrict annotations

    private val configuredAnnotations: Map<ClassId, List<PermissionFactory>>?
        by lazy { compilerConfig[ICCommandLineProcessor.RESTRICT_ANNOTATIONS] }

    context(reporter: DiagnosticReporter)
    private fun getRestrictAnnotationPermissions(annotationClass: FirRegularClass): List<PermissionFactory> {
        if (annotationClass.classKind != ClassKind.ANNOTATION_CLASS) {
            throw IllegalArgumentException("Not an annotation class: ${annotationClass.name}")
        }
        annotationClass.icRestrictAnnotationPermissions?.let { return it }
        val factories = mutableListOf<PermissionFactory>()
        val metaAnnotation = annotationClass.annotations.find {
            it.toAnnotationClassIdSafe(session) == ICNames.ANNOT_RESTRICT_ANNOTATION
        }
        if (metaAnnotation != null) {
            context(ResolveContext(null)) {
                metaAnnotation.forEachVarargArgument<FirLiteralExpression>(ICNames.ARG_PERMISSIONS, session) { arg ->
                    PermissionFactory.fromTemplate(arg.value as String, arg.source)?.let { factories += it }
                }
            }
        }
        configuredAnnotations?.get(annotationClass.classId)?.let { factories.addAll(it) }
        annotationClass.icRestrictAnnotationPermissions = factories
        return factories
    }

    // Phase 2: resolve top-level declaration permissions

    override val scopeSession: ScopeSession = ScopeSession()

    @OptIn(SymbolInternals::class)
    context(reporter: DiagnosticReporter)
    private fun getPermissionsForAnnotation(destination: MutableCollection<String>, annotation: FirAnnotation) {
        when (annotation.toAnnotationClassIdSafe(session)) {
            null -> {}

            ICNames.ANNOT_RESTRICT -> {
                annotation.forEachVarargArgument<FirLiteralExpression>(ICNames.ARG_PERMISSIONS, session) {
                    destination += it.value as String
                }
            }

            else -> {
                val symbol = annotation.toAnnotationClassLikeSymbol(session)!!
                context(ResolveContext(null)) {
                    val argumentProvider = AnnotationArgumentProvider(annotation)
                    getRestrictAnnotationPermissions(symbol.fir as FirRegularClass).mapNotNullTo(destination) {
                        it.build(argumentProvider)
                    }
                }
            }
        }
    }

    private inner class AnnotationArgumentProvider(private val annotation: FirAnnotation) : PermissionArgumentProvider {
        // this could be memoized, but it's unlikely that a single argument will be used more than once
        context(context: DiagnosticContext, reporter: DiagnosticReporter)
        override fun get(key: String, useSite: AbstractKtSourceElement?): String? {
            val argument = annotation.findArgumentByName(Name.identifier(key)) ?: run {
                reporter.reportOn(useSite, ICDiagnostics.KIC_NO_SUCH_PERMISSION_ARGUMENT, key)
                return null
            }
            val asLiteral = argument.evaluateAs<FirLiteralExpression>(session)
            if (asLiteral != null) {
                return asLiteral.value.toString()
            }
            val asEnum = argument.extractEnumValueArgumentInfo()
            if (asEnum != null) {
                return asEnum.enumEntryName.asString()
            }
            reporter.reportOn(useSite, ICDiagnostics.KIC_INVALID_PERMISSION_ARGUMENT_VALUE, key)
            return null
        }
    }

    @OptIn(SymbolInternals::class)
    context(reporter: DiagnosticReporter)
    fun getDeclarationAnnotatedPermissions(declaration: FirDeclaration): Set<String> {
        declaration.icDeclarationPermissions?.let { return it }
        val permissions = mutableSetOf<String>()
        declaration.annotations.forEach { getPermissionsForAnnotation(permissions, it) }
        if (declaration is FirConstructor) {
            val ownerTypeRef = declaration.returnTypeRef
            if (ownerTypeRef is FirResolvedTypeRef) {
                ownerTypeRef.coneType.toClassLikeSymbol()?.fir?.let { ownerClass ->
                    permissions += getDeclarationAnnotatedPermissions(ownerClass)
                }
            }
        }
        declaration.icDeclarationPermissions = permissions.ifEmpty { emptySet() }
        return permissions
    }

    context(reporter: DiagnosticReporter)
    private fun transformAnnotatedTypesLater(
        type: ConeKotlinType,
        state: LeafTypeTransformState
    ): Later<ConeKotlinType> = context(session.typeContext) {
        type.transformLeaves(state) { leaf ->
            if (leaf !is ConeClassLikeType) return@transformLeaves Later.Now(leaf)
            val permissions = mutableSetOf<String>()
            leaf.typeAnnotations.forEach { getPermissionsForAnnotation(permissions, it) }
            return@transformLeaves leaf.typeArguments.traverseLater { argument ->
                argument.mapTypeLater { transformAnnotatedTypesLater(it, state) }
            }.map { arguments ->
                ConeClassLikeTypeImpl(
                    leaf.lookupTag,
                    arguments.toTypedArray(),
                    leaf.isMarkedNullable,
                    leaf.attributes.add(FunctionTypePermissionsAttribute(PermissionP.of(permissions)))
                )
            }
        }
    }

    context(reporter: DiagnosticReporter)
    fun transformAnnotatedTypes(type: ConeKotlinType): ConeKotlinType =
        transformAnnotatedTypesLater(type, LeafTypeTransformState()).force()

    context(reporter: DiagnosticReporter)
    fun resolveReturnTypePermissions(typedDeclaration: FirCallableDeclaration): PermissionP {
        val typeRef = typedDeclaration.returnTypeRef as FirResolvedTypeRef
        val type = typeRef.coneType
        type.attributes.icFunctionTypePermissions?.let { return it.permissions }
        val newType = transformAnnotatedTypes(type)
        typedDeclaration.replaceReturnTypeRef(
            buildResolvedTypeRefCopy(typeRef) {
                coneType = newType
            }
        )
        return newType.attributes.icFunctionTypePermissions?.permissions ?: PermissionP.EMPTY
    }

    context(reporter: DiagnosticReporter)
    fun resolveReturnTypePermissions(typedDeclaration: FirReceiverParameter): PermissionP {
        val typeRef = typedDeclaration.typeRef as FirResolvedTypeRef
        val type = typeRef.coneType
        if (type.typeArguments.isEmpty()) return PermissionP.EMPTY
        type.attributes.icFunctionTypePermissions?.let { return it.permissions }
        val newType = transformAnnotatedTypes(type)
        typedDeclaration.replaceTypeRef(
            buildResolvedTypeRefCopy(typeRef) {
                coneType = newType
            }
        )
        return newType.attributes.icFunctionTypePermissions?.permissions ?: PermissionP.EMPTY
    }

    context(reporter: DiagnosticReporter)
    fun resolveReturnTypePermissions(typedExpression: FirExpression): PermissionP {
        val type = typedExpression.resolvedType
        if (type.typeArguments.isEmpty()) return PermissionP.EMPTY
        type.attributes.icFunctionTypePermissions?.let { return it.permissions }
        val newType = transformAnnotatedTypes(type)
        typedExpression.replaceConeTypeOrNull(newType)
        return newType.attributes.icFunctionTypePermissions?.permissions ?: PermissionP.EMPTY
    }

    @OptIn(SymbolInternals::class)
    context(context: DiagnosticContext, reporter: DiagnosticReporter)
    fun resolveCallable(
        callableReference: FirNamedReference,
        dispatchReceiver: FirExpression?
    ): Pair<String, Set<String>?> {
        if (dispatchReceiver != null) {
            if (dispatchReceiver.resolvedType.isSomeFunctionType(session)) { // TODO check that it's actually invoke()
                val permissions = when (val p = resolveReturnTypePermissions(dispatchReceiver)) {
                    is PermissionP.Some -> p.permissions
                    PermissionP.Poison -> {
                        reporter.reportOn(dispatchReceiver.source, ICDiagnostics.KIC_POISON_FUNCTION_TYPE)
                        null
                    }
                }
                // resolvedType may have changed in resolveFunctionTypePermissions
                return dispatchReceiver.resolvedType.renderReadable() to permissions
            }
        }
        return Pair(
            callableReference.name.asStringStripSpecialMarkers(),
            callableReference.resolved?.resolvedSymbol?.let { getDeclarationAnnotatedPermissions(it.fir) }
        )
    }

    @OptIn(SymbolInternals::class)
    context(reporter: DiagnosticReporter)
    fun resolveCallableReferenceType(
        type: ConeKotlinType,
        callableReference: FirNamedReference,
        dispatchReceiver: FirExpression?
    ): ConeKotlinType {
        if (dispatchReceiver != null) {
            if (dispatchReceiver.resolvedType.isSomeFunctionType(session)) { // TODO check that it's actually invoke()
                resolveReturnTypePermissions(dispatchReceiver)
                // resolvedType may have changed in resolveFunctionTypePermissions
                return dispatchReceiver.resolvedType
            }
        }
        val newType = transformAnnotatedTypes(type)
        val permissions = callableReference.resolved?.resolvedSymbol?.let { getDeclarationAnnotatedPermissions(it.fir) }
        return if (permissions == null) newType else {
            newType.withAttributes(
                newType.attributes.add(FunctionTypePermissionsAttribute(PermissionP.of(permissions)))
            )
        }
    }

    context(reporter: DiagnosticReporter)
    fun resolveLambda(lambda: FirAnonymousFunction) {
        val typeRef = lambda.typeRef as? FirResolvedTypeRef ?: return
        val newType = transformAnnotatedTypes(typeRef.coneType)
        lambda.replaceTypeRef(
            buildResolvedTypeRefCopy(typeRef) {
                coneType = newType.withAttributes(
                    newType.attributes.add(
                        FunctionTypePermissionsAttribute(PermissionP.of(getDeclarationAnnotatedPermissions(lambda)))
                    )
                )
            }
        )
    }

    private fun getSamMethod(samClass: FirRegularClass): Pair<FirNamedFunction, ConeKotlinType>? {
        samClass.icSamFunctionType?.let { return it.orNull() }
        val result = run {
            if (!samClass.status.isFun) return@run null
            val samMethod = samClass.getSingleAbstractMethodOrNull() ?: return@run null
            return@run samMethod to samMethod.getFunctionTypeForAbstractMethod(session)
        }
        samClass.icSamFunctionType = nullableToMaybe(result)
        return result
    }

    @OptIn(SymbolInternals::class)
    context(reporter: DiagnosticReporter)
    fun resolveSamFunctionType(samType: ConeKotlinType): ConeKotlinType? {
        when (samType) {
            is ConeClassLikeType -> {
                val samClass = samType.fullyExpandedType().lookupTag.toRegularClassSymbol()?.fir
                    ?: return null
                val (samMethod, unsubstitutedFunctionType) = getSamMethod(samClass) ?: return null
                val rawFunctionType = samClass.buildSubstitutorWithUpperBounds(samType)
                    .substituteOrNull(unsubstitutedFunctionType) ?: unsubstitutedFunctionType
                val functionType = transformAnnotatedTypes(
                    rawFunctionType.withNullabilityOf(samType, session.typeContext)
                )
                return functionType.withAttributes(
                    functionType.attributes.add(
                        FunctionTypePermissionsAttribute(PermissionP.of(getDeclarationAnnotatedPermissions(samMethod)))
                    )
                )
            }

            is ConeFlexibleType -> {
                val lowerType = resolveSamFunctionType(samType.lowerBound) ?: return null
                if (samType.isTrivial) {
                    return lowerType.lowerBoundIfFlexible().toTrivialFlexibleType(session.typeContext)
                } else {
                    val upperType = resolveSamFunctionType(samType.upperBound) ?: return null
                    return ConeFlexibleType(
                        lowerType.lowerBoundIfFlexible(),
                        upperType.upperBoundIfFlexible(),
                        isTrivial = false
                    )
                }
            }

            is ConeCapturedType -> return samType.constructor.lowerType?.let { resolveSamFunctionType(it) }
            else -> return null
        }
    }

    private inner class ResolvingSubstitutor(
        private val upstream: ConeSubstitutor,
        private val reporter: DiagnosticReporter
    ) : ConeSubstitutor() {
        override fun substituteArgument(projection: ConeTypeProjection, index: Int): ConeTypeProjection? =
            context(reporter) {
                upstream.substituteArgument(projection, index)?.mapType { transformAnnotatedTypes(it) }
            }

        override fun substituteOrNull(type: ConeKotlinType): ConeKotlinType? = context(reporter) {
            upstream.substituteOrNull(type)?.let { transformAnnotatedTypes(it) }
        }
    }

    fun getSubstitutor(reporter: DiagnosticReporter, expression: FirQualifiedAccessExpression): ConeSubstitutor? =
        expression.createConeSubstitutorFromTypeArguments(session)?.let { ResolvingSubstitutor(it, reporter) }

    // Phase 3: permission checking

    private val checkedFiles: MutableMap<FirFile, List<Pair<KtDiagnostic?, DiagnosticContext>>> = mutableMapOf()

    context(reporter: DiagnosticReporter)
    fun checkFile(file: FirFile) {
        checkedFiles[file]?.let {
            it.forEach { (diagnostic, context) -> reporter.report(diagnostic, context) }
            return
        }
        val teeReporter = TeeReporter(reporter)
        checkedFiles[file] = teeReporter.diagnosticLog
        val resolveContext = ResolveContext(file)
        try {
            file.accept(PermissionChecker(this, resolveContext, teeReporter), PermissionChecker.Context())
        } catch (e: Exception) {
            throw e.wrapIntoFileAnalysisExceptionIfNeeded(resolveContext.containingFilePath, file.source) {
                file.sourceFileLinesMapping?.getLineAndColumnByOffset(it)
            }
        }
    }

    private class TeeReporter(private val delegate: DiagnosticReporter) : DiagnosticReporter() {
        val diagnosticLog: MutableList<Pair<KtDiagnostic?, DiagnosticContext>> = mutableListOf()

        override val hasErrors: Boolean
            get() = delegate.hasErrors

        override fun report(diagnostic: KtDiagnostic?, context: DiagnosticContext) {
            diagnosticLog += diagnostic to context
            delegate.report(diagnostic, context)
        }
    }
}
