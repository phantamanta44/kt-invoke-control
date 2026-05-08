package st.evening.kt.invokecontrol.kplugin.resolve

import org.jetbrains.kotlin.AbstractKtSourceElement
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.diagnostics.DiagnosticContext
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.SessionAndScopeSessionHolder
import org.jetbrains.kotlin.fir.declarations.FirAnonymousFunction
import org.jetbrains.kotlin.fir.declarations.FirCallableDeclaration
import org.jetbrains.kotlin.fir.declarations.FirConstructor
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.declarations.FirNamedFunction
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.FirPropertyAccessor
import org.jetbrains.kotlin.fir.declarations.FirReceiverParameter
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.FirTypeAlias
import org.jetbrains.kotlin.fir.declarations.findArgumentByName
import org.jetbrains.kotlin.fir.declarations.toAnnotationClassIdSafe
import org.jetbrains.kotlin.fir.declarations.unwrapVarargValue
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.expressions.FirCall
import org.jetbrains.kotlin.fir.expressions.FirCallableReferenceAccess
import org.jetbrains.kotlin.fir.expressions.FirContextArgumentListOwner
import org.jetbrains.kotlin.fir.expressions.FirDelegatedConstructorCall
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.FirLiteralExpression
import org.jetbrains.kotlin.fir.expressions.FirOperation
import org.jetbrains.kotlin.fir.expressions.FirPropertyAccessExpression
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.expressions.FirResolvable
import org.jetbrains.kotlin.fir.expressions.FirResolvedQualifier
import org.jetbrains.kotlin.fir.expressions.FirSamConversionExpression
import org.jetbrains.kotlin.fir.expressions.FirTypeOperatorCall
import org.jetbrains.kotlin.fir.expressions.argument
import org.jetbrains.kotlin.fir.expressions.arguments
import org.jetbrains.kotlin.fir.references.FirNamedReference
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.resolve.getContainingClassSymbol
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.resolve.toRegularClassSymbol
import org.jetbrains.kotlin.fir.scopes.ScopeFunctionRequiresPrewarm
import org.jetbrains.kotlin.fir.scopes.getDirectOverriddenFunctions
import org.jetbrains.kotlin.fir.scopes.getDirectOverriddenProperties
import org.jetbrains.kotlin.fir.scopes.unsubstitutedScope
import org.jetbrains.kotlin.fir.symbols.ConeTypeParameterLookupTag
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirConstructorSymbol
import org.jetbrains.kotlin.fir.types.ConeCapturedType
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.ConeDefinitelyNotNullType
import org.jetbrains.kotlin.fir.types.ConeFlexibleType
import org.jetbrains.kotlin.fir.types.ConeIntegerLiteralType
import org.jetbrains.kotlin.fir.types.ConeIntersectionType
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.ConeRigidType
import org.jetbrains.kotlin.fir.types.ConeSimpleKotlinType
import org.jetbrains.kotlin.fir.types.ConeTypeParameterType
import org.jetbrains.kotlin.fir.types.ConeTypeVariableType
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.types.isSomeFunctionType
import org.jetbrains.kotlin.fir.types.renderForDebugging
import org.jetbrains.kotlin.fir.types.resolvedType
import org.jetbrains.kotlin.fir.types.typeContext
import org.jetbrains.kotlin.fir.visitors.FirDefaultVisitor
import org.jetbrains.kotlin.types.AbstractTypeChecker
import org.jetbrains.kotlin.types.TypeCheckerState
import org.jetbrains.kotlin.types.model.TypeCheckerProviderContext
import org.jetbrains.kotlin.types.model.TypeConstructorMarker
import org.jetbrains.kotlin.types.model.TypeSystemContext
import org.jetbrains.kotlin.types.model.TypeVariance
import org.jetbrains.kotlin.types.model.argumentsCount
import org.jetbrains.kotlin.types.model.asArgumentList
import org.jetbrains.kotlin.types.model.get
import org.jetbrains.kotlin.types.model.getArgument
import org.jetbrains.kotlin.types.model.getParameter
import org.jetbrains.kotlin.types.model.getType
import org.jetbrains.kotlin.types.model.getVariance
import org.jetbrains.kotlin.types.model.isAnyConstructor
import org.jetbrains.kotlin.types.model.isNothingConstructor
import org.jetbrains.kotlin.types.model.parametersCount
import org.jetbrains.kotlin.types.model.size
import org.jetbrains.kotlin.types.model.typeConstructor
import org.jetbrains.kotlin.util.wrapIntoSourceCodeAnalysisExceptionIfNeeded
import st.evening.kt.invokecontrol.kplugin.ICDiagnostics
import st.evening.kt.invokecontrol.kplugin.ICNames
import st.evening.kt.invokecontrol.kplugin.permission.PermissionP
import st.evening.kt.invokecontrol.kplugin.permission.PermissionSet
import st.evening.kt.invokecontrol.kplugin.permission.icFunctionTypePermissions
import st.evening.kt.invokecontrol.kplugin.util.forEachVarargArgument
import st.evening.kt.invokecontrol.kplugin.util.unwrapClassType

internal class PermissionChecker(
    private val resolveService: ICResolveService,
    private val dContext: DiagnosticContext,
    private val reporter: DiagnosticReporter
) : FirDefaultVisitor<Nothing?, PermissionChecker.Context>(), SessionAndScopeSessionHolder by resolveService {
    class Context(val parentContext: Context?, val localPermissions: Set<String>) {
        constructor() : this(null, emptySet())

        val permissions: Set<String>
            by lazy { parentContext?.permissions?.let { it + localPermissions } ?: localPermissions }
    }

    private inline fun void(action: () -> Unit): Nothing? {
        action()
        return null
    }

    private inline fun withErrorHandling(source: KtSourceElement?, action: () -> Unit) {
        try {
            action()
        } catch (e: Exception) {
            throw e.wrapIntoSourceCodeAnalysisExceptionIfNeeded(source)
        } catch (e: StackOverflowError) {
            throw RuntimeException(e).wrapIntoSourceCodeAnalysisExceptionIfNeeded(source)
        }
    }

    private inline fun analyze(element: FirElement, data: Context, action: () -> Unit): Nothing? {
        visitElement(element, data)
        withErrorHandling(element.source) {
            action()
        }
        return null
    }

    override fun visitElement(element: FirElement, data: Context): Nothing? = void {
        element.acceptChildren(this, data)
    }

    private inline fun extendAndCheck(
        declaration: FirDeclaration,
        parentContext: Context,
        check: (Context) -> Unit
    ): Nothing? = void {
        val subContext = Context(
            parentContext,
            context(dContext, reporter) {
                resolveService.getDeclarationAnnotatedPermissions(declaration)
            }
        )
        withErrorHandling(declaration.source) {
            check(subContext)
        }
        visitElement(declaration, subContext)
    }

    private fun checkReferencedTypePermissions(
        source: AbstractKtSourceElement?,
        context: Context,
        requiredPermissions: PermissionSet
    ) {
        if (requiredPermissions.isEmpty()) return
        val missingPermissions = requiredPermissions - context.permissions
        if (missingPermissions.isEmpty()) return
        context(dContext) {
            reporter.reportOn(
                source,
                ICDiagnostics.KIC_LEAKY_DECLARATION,
                missingPermissions.getPermissions(),
                missingPermissions.getProvenance()
            )
        }
    }

    @OptIn(SymbolInternals::class)
    private fun PermissionSet.Builder.addFromSimpleKotlinType(type: ConeSimpleKotlinType) {
        when (type) {
            is ConeCapturedType -> {
                val constructor = type.constructor
                constructor.lowerType?.let { addFromConeType(it) } // TODO is this redundant?
                constructor.supertypes?.forEach { addFromConeType(it) }
            }

            is ConeIntegerLiteralType -> {} // built-in integer types should have no restrictions

            is ConeIntersectionType -> type.intersectedTypes.forEach { addFromConeType(it) }

            is ConeClassLikeType -> {
                val symbol = type.toRegularClassSymbol() ?: return
                addAll(
                    context(reporter) {
                        resolveService.getDeclarationAnnotatedPermissions(symbol.fir)
                    },
                    symbol.name.asStringStripSpecialMarkers()
                )
            }

            is ConeTypeParameterType -> {
                type.lookupTag.typeParameterSymbol.resolvedBounds.forEach {
                    addFromType(it)
                }
            }

            is ConeTypeVariableType -> {
                (type.typeConstructor.originalTypeParameter as? ConeTypeParameterLookupTag)
                    ?.typeParameterSymbol?.resolvedBounds?.forEach {
                        addFromType(it)
                    }
            }

            else -> throw UnsupportedOperationException(type.renderForDebugging())
        }
    }

    private fun PermissionSet.Builder.addFromRigidType(type: ConeRigidType) {
        when (type) {
            is ConeDefinitelyNotNullType -> addFromSimpleKotlinType(type.original)
            is ConeSimpleKotlinType -> addFromSimpleKotlinType(type)
        }
    }

    private fun PermissionSet.Builder.addFromConeType(type: ConeKotlinType) {
        when (type) {
            is ConeRigidType -> addFromRigidType(type)
            is ConeFlexibleType -> {
                addFromRigidType(type.lowerBound)
                addFromRigidType(type.upperBound)
            }
        }
    }

    private fun PermissionSet.Builder.addFromType(typeRef: FirTypeRef) {
        if (typeRef is FirResolvedTypeRef) {
            addFromConeType(typeRef.coneType)
        }
    }

    private inline fun checkReferencedTypePermissions(
        source: AbstractKtSourceElement?,
        context: Context,
        builderAction: (PermissionSet.Builder) -> Unit
    ) {
        checkReferencedTypePermissions(source, context, PermissionSet.build { builder ->
            builderAction(builder)
        })
    }

    private inline fun extendAndCheckReferencedTypePermissions(
        declaration: FirDeclaration,
        parentContext: Context,
        builderAction: (PermissionSet.Builder) -> Unit
    ): Nothing? = extendAndCheck(declaration, parentContext) { context ->
        checkReferencedTypePermissions(declaration.source, context, builderAction)
    }

    private fun substitute(substitutor: ConeSubstitutor?, type: ConeKotlinType): ConeKotlinType =
        substitutor?.substituteOrNull(type) ?: type

    private sealed interface FunctionTypeCheckResult {
        object Success : FunctionTypeCheckResult
        class Leak(val leakedPermissions: Set<String>) : FunctionTypeCheckResult
        class Poison(val source: AbstractKtSourceElement?) : FunctionTypeCheckResult
        object Pass : FunctionTypeCheckResult
    }

    context(typeContext: TypeSystemContext)
    private fun checkTypesEqual(
        destType: ConeKotlinType,
        destSource: AbstractKtSourceElement?,
        valueType: ConeKotlinType,
        valueSource: AbstractKtSourceElement?,
        substitutor: ConeSubstitutor?
    ): FunctionTypeCheckResult {
        val destClassType = substitute(substitutor, destType)
            .unwrapClassType()?.fullyExpandedType() ?: return FunctionTypeCheckResult.Pass
        val valueClassType = substitute(substitutor, valueType)
            .unwrapClassType()?.fullyExpandedType() ?: return FunctionTypeCheckResult.Pass
        if (!typeContext.areEqualTypeConstructors(destClassType.typeConstructor(), valueClassType.typeConstructor())) {
            return FunctionTypeCheckResult.Pass
        }

        if (destClassType.isSomeFunctionType(session)) {
            val destAttribute = destClassType.attributes.icFunctionTypePermissions
                ?: throw IllegalStateException("Destination missing attribute: ${destClassType.renderForDebugging()}")
            val valueAttribute = valueClassType.attributes.icFunctionTypePermissions
                ?: throw IllegalStateException("Value missing attribute: ${destClassType.renderForDebugging()}")
            when (val destP = destAttribute.permissions) {
                PermissionP.Poison -> return FunctionTypeCheckResult.Poison(destSource)
                is PermissionP.Some -> {
                    when (val valueP = valueAttribute.permissions) {
                        PermissionP.Poison -> return FunctionTypeCheckResult.Poison(valueSource)
                        is PermissionP.Some -> {
                            val destPermissions = destP.permissions
                            val valuePermissions = valueP.permissions
                            if (destPermissions != valuePermissions) {
                                val leakedPermissions = destPermissions.toMutableSet()
                                valuePermissions.forEach {
                                    if (!leakedPermissions.remove(it)) {
                                        leakedPermissions += it
                                    }
                                }
                                return FunctionTypeCheckResult.Leak(leakedPermissions)
                            }
                        }
                    }
                }
            }
        }
        return FunctionTypeCheckResult.Success
    }

    context(typeContext: TypeSystemContext)
    private fun isSimpleSubtype(destTypeCtor: TypeConstructorMarker, valueTypeCtor: TypeConstructorMarker): Boolean =
        destTypeCtor.isAnyConstructor() || valueTypeCtor.isNothingConstructor() ||
            (typeContext.areEqualTypeConstructors(destTypeCtor, valueTypeCtor) && destTypeCtor.parametersCount() == 0)

    context(typeContext: TypeSystemContext, state: TypeCheckerState)
    private fun checkIsSubtype(
        destType: ConeKotlinType,
        destSource: AbstractKtSourceElement?,
        valueType: ConeKotlinType,
        valueSource: AbstractKtSourceElement?,
        substitutor: ConeSubstitutor?
    ): FunctionTypeCheckResult {
        if (isSimpleSubtype(destType.typeConstructor(), valueType.typeConstructor())) {
            return FunctionTypeCheckResult.Success
        }

        val destClassType = substitute(substitutor, destType)
            .unwrapClassType()?.fullyExpandedType() ?: return FunctionTypeCheckResult.Pass
        val valueClassType = substitute(substitutor, valueType)
            .unwrapClassType()?.fullyExpandedType() ?: return FunctionTypeCheckResult.Pass

        val destCtor = destClassType.typeConstructor()
        if (isSimpleSubtype(destCtor, valueClassType.typeConstructor())) return FunctionTypeCheckResult.Success

        if (destClassType.isSomeFunctionType(session)) {
            val destAttribute = destClassType.attributes.icFunctionTypePermissions
                ?: throw IllegalStateException("Destination missing attribute: ${destClassType.renderForDebugging()}")
            val valueAttribute = valueClassType.attributes.icFunctionTypePermissions
                ?: throw IllegalStateException("Value missing attribute: ${destClassType.renderForDebugging()}")
            when (val destP = destAttribute.permissions) {
                PermissionP.Poison -> return FunctionTypeCheckResult.Poison(destSource)
                is PermissionP.Some -> {
                    when (val valueP = valueAttribute.permissions) {
                        PermissionP.Poison -> return FunctionTypeCheckResult.Poison(valueSource)
                        is PermissionP.Some -> {
                            val destPermissions = destP.permissions
                            val valuePermissions = valueP.permissions
                            if (!destPermissions.containsAll(valuePermissions)) {
                                return FunctionTypeCheckResult.Leak(valuePermissions - destPermissions)
                            }
                        }
                    }
                }
            }
        }

        AbstractTypeChecker.findCorrespondingSupertypes(valueClassType, destCtor).forEach { superTypeArguments ->
            val valueArguments = superTypeArguments.asArgumentList()
            val valueArgumentCount = valueArguments.size()
            val parameterCount = destCtor.parametersCount()
            if (valueArgumentCount != parameterCount || valueArgumentCount != destClassType.argumentsCount()) {
                return FunctionTypeCheckResult.Pass
            }

            for (index in 0..<parameterCount) {
                val destArg = destClassType.getArgument(index)
                val destArgType = (destArg.getType() ?: continue) as ConeKotlinType
                val valueArgType = valueArguments[index].getType() as ConeKotlinType
                val variance = AbstractTypeChecker.effectiveVariance(
                    destCtor.getParameter(index).getVariance(),
                    destArg.getVariance()
                ) ?: continue
                val result = when (variance) {
                    TypeVariance.INV -> checkTypesEqual(valueArgType, valueSource, destArgType, destSource, substitutor)
                    TypeVariance.IN -> checkIsSubtype(valueArgType, valueSource, destArgType, destSource, substitutor)
                    TypeVariance.OUT -> checkIsSubtype(destArgType, destSource, valueArgType, valueSource, substitutor)
                }
                if (result != FunctionTypeCheckResult.Success) return result
            }
        }
        return FunctionTypeCheckResult.Success
    }

    context(providerContext: TypeCheckerProviderContext)
    private fun checkAssignment(
        destination: FirCallableDeclaration,
        value: FirExpression,
        substitutor: ConeSubstitutor?
    ) {
        context(reporter) {
            resolveService.resolveReturnTypePermissions(destination)
            resolveService.resolveReturnTypePermissions(value)
        }
        val state = providerContext.newTypeCheckerState(
            errorTypesEqualToAnything = false,
            stubTypesEqualToAnything = false
        )
        val result = context(state, state.typeSystemContext) {
            checkIsSubtype(
                destination.returnTypeRef.coneType,
                destination.source,
                value.resolvedType,
                value.source,
                substitutor
            )
        }
        when (result) {
            is FunctionTypeCheckResult.Leak -> context(dContext) {
                reporter.reportOn(value.source, ICDiagnostics.KIC_LEAKY_ASSIGNMENT, result.leakedPermissions)
            }

            is FunctionTypeCheckResult.Poison -> context(dContext) {
                reporter.reportOn(result.source, ICDiagnostics.KIC_POISON_FUNCTION_TYPE)
            }

            else -> {}
        }
    }

    context(providerContext: TypeCheckerProviderContext)
    private fun checkAssignment(
        destination: FirReceiverParameter,
        value: FirExpression,
        substitutor: ConeSubstitutor?
    ) {
        context(reporter) {
            resolveService.resolveReturnTypePermissions(destination)
            resolveService.resolveReturnTypePermissions(value)
        }
        val state = providerContext.newTypeCheckerState(
            errorTypesEqualToAnything = false,
            stubTypesEqualToAnything = false
        )
        val result = context(state, state.typeSystemContext) {
            checkIsSubtype(
                destination.typeRef.coneType,
                destination.source,
                value.resolvedType,
                value.source,
                substitutor
            )
        }
        when (result) {
            is FunctionTypeCheckResult.Leak -> context(dContext) {
                reporter.reportOn(value.source, ICDiagnostics.KIC_LEAKY_ASSIGNMENT, result.leakedPermissions)
            }

            is FunctionTypeCheckResult.Poison -> context(dContext) {
                reporter.reportOn(result.source, ICDiagnostics.KIC_POISON_FUNCTION_TYPE)
            }

            else -> {}
        }
    }

    private fun checkAccessPermissions(
        expression: FirExpression,
        referentName: String,
        requiredPermissions: Set<String>?,
        context: Context
    ) {
        val grant = expression.annotations.find { it.toAnnotationClassIdSafe(session) == ICNames.ANNOT_UNCHECKED }
        if (!requiredPermissions.isNullOrEmpty()) {
            val remainingPermissions = requiredPermissions.toMutableSet().apply { removeAll(context.permissions) }
            if (grant != null) {
                val redundantPermissions = mutableSetOf<String>()
                grant.forEachVarargArgument<FirLiteralExpression>(ICNames.ARG_PERMISSIONS, session) {
                    val permission = it.value as String
                    if (!remainingPermissions.remove(permission)) {
                        redundantPermissions += permission
                    }
                }
                if (redundantPermissions.isNotEmpty()) {
                    context(dContext) {
                        reporter.reportOn(grant.source, ICDiagnostics.KIC_REDUNDANT_UNCHECKED, redundantPermissions)
                    }
                }
            }
            if (remainingPermissions.isNotEmpty()) {
                context(dContext) {
                    reporter.reportOn(
                        expression.source,
                        ICDiagnostics.KIC_INSUFFICIENT_PERMISSIONS,
                        remainingPermissions,
                        setOf(referentName)
                    )
                }
            }
        } else if (grant != null) {
            val redundantPermissions = mutableSetOf<String>()
            grant.forEachVarargArgument<FirLiteralExpression>(ICNames.ARG_PERMISSIONS, session) {
                redundantPermissions += it.value as String
            }
            if (redundantPermissions.isNotEmpty()) {
                context(dContext) {
                    reporter.reportOn(grant.source, ICDiagnostics.KIC_REDUNDANT_UNCHECKED, redundantPermissions)
                }
            }
        }
    }

    @OptIn(SymbolInternals::class)
    private fun <E> checkAccessExpression(expression: E, context: Context) where E : FirExpression, E : FirResolvable {
        // check call permissions
        val calleeReference = expression.calleeReference as? FirNamedReference ?: return
        val (calleeName, requiredPermissions) = context(dContext, reporter) {
            resolveService.resolveCallable(
                calleeReference,
                (expression as? FirQualifiedAccessExpression)?.dispatchReceiver
            )
        }
        checkAccessPermissions(expression, calleeName, requiredPermissions, context)

        // check argument types
        if (calleeReference is FirResolvedNamedReference) {
            val callee = calleeReference.resolvedSymbol.fir as FirCallableDeclaration
            context(session.typeContext) {
                val substitutor: ConeSubstitutor?
                if (expression is FirQualifiedAccessExpression) {
                    substitutor = resolveService.getSubstitutor(reporter, expression)
                    callee.receiverParameter?.let { receiverParameter ->
                        expression.extensionReceiver?.let {
                            checkAssignment(receiverParameter, it, substitutor)
                        }
                    }
                } else {
                    substitutor = null
                }
                if (expression is FirContextArgumentListOwner) {
                    val contextParameters = callee.contextParameters
                    val contextArguments = expression.contextArguments
                    if (contextParameters.size == contextArguments.size) {
                        contextParameters.forEachIndexed { index, parameter ->
                            checkAssignment(parameter, contextArguments[index], substitutor)
                        }
                    }
                }
                if (callee is FirFunction) {
                    val valueParameters = callee.valueParameters
                    val valueArguments = (expression as FirCall).arguments
                    if (valueParameters.size == valueArguments.size) {
                        valueParameters.forEachIndexed { index, parameter ->
                            checkAssignment(parameter, valueArguments[index], substitutor)
                        }
                    }
                }
            }
        }
    }

    // Declarations

    override fun visitFile(file: FirFile, data: Context): Nothing? = extendAndCheck(file, data) {}

    override fun visitRegularClass(regularClass: FirRegularClass, data: Context): Nothing? =
        extendAndCheckReferencedTypePermissions(regularClass, data) { builder ->
            regularClass.superTypeRefs.forEach { builder.addFromType(it) }
        }

    override fun visitTypeAlias(typeAlias: FirTypeAlias, data: Context): Nothing? =
        extendAndCheckReferencedTypePermissions(typeAlias, data) { builder ->
            builder.addFromType(typeAlias.expandedTypeRef)
        }

    @OptIn(SymbolInternals::class, ScopeFunctionRequiresPrewarm::class)
    override fun visitProperty(property: FirProperty, data: Context): Nothing? = void {
        extendAndCheckReferencedTypePermissions(property, data) { builder ->
            builder.addFromType(property.returnTypeRef)
            property.receiverParameter?.let { builder.addFromType(it.typeRef) }
            property.contextParameters.forEach {
                builder.addFromType(it.returnTypeRef)
            }
            val containingClassSymbol = property.getContainingClassSymbol()
            if (containingClassSymbol is FirClassSymbol<*>) {
                val scope = containingClassSymbol.fir.unsubstitutedScope(
                    session, resolveService.scopeSession, true, FirResolvePhase.ANNOTATION_ARGUMENTS
                )
                scope.processPropertiesByName(property.name) {}
                scope.getDirectOverriddenProperties(property.symbol, true).forEach {
                    builder.addAll(
                        context(reporter) {
                            resolveService.getDeclarationAnnotatedPermissions(it.fir)
                        },
                        it.name.asStringStripSpecialMarkers()
                    )
                }
            }
        }
        withErrorHandling(property.source) {
            property.initializer?.let {
                context(session.typeContext) {
                    checkAssignment(property, it, null)
                }
            }
        }
    }

    @OptIn(SymbolInternals::class, ScopeFunctionRequiresPrewarm::class)
    override fun visitNamedFunction(namedFunction: FirNamedFunction, data: Context): Nothing? =
        extendAndCheckReferencedTypePermissions(namedFunction, data) { builder ->
            builder.addFromType(namedFunction.returnTypeRef)
            namedFunction.valueParameters.forEach {
                builder.addFromType(it.returnTypeRef)
            }
            namedFunction.receiverParameter?.let { builder.addFromType(it.typeRef) }
            namedFunction.contextParameters.forEach {
                builder.addFromType(it.returnTypeRef)
            }

            val containingClassSymbol = namedFunction.getContainingClassSymbol()
            if (containingClassSymbol is FirClassSymbol<*>) {
                val scope = containingClassSymbol.fir.unsubstitutedScope(
                    session, resolveService.scopeSession, true, FirResolvePhase.ANNOTATION_ARGUMENTS
                )
                scope.processFunctionsByName(namedFunction.name) {}
                scope.getDirectOverriddenFunctions(namedFunction.symbol, true).forEach {
                    builder.addAll(
                        context(reporter) {
                            resolveService.getDeclarationAnnotatedPermissions(it.fir)
                        },
                        it.name.asStringStripSpecialMarkers()
                    )
                }
                // FIXME ensure overrides in classes implementing function types have correct permissions
            }
        }

    override fun visitConstructor(constructor: FirConstructor, data: Context): Nothing? =
        extendAndCheckReferencedTypePermissions(constructor, data) { builder ->
            constructor.valueParameters.forEach {
                builder.addFromType(it.returnTypeRef)
            }
            constructor.contextParameters.forEach { // at time of writing, constructors don't support context parameters
                builder.addFromType(it.returnTypeRef)
            }
        }

    override fun visitAnonymousFunction(anonymousFunction: FirAnonymousFunction, data: Context): Nothing? =
        extendAndCheck(anonymousFunction, data) {
            context(reporter) {
                resolveService.resolveLambda(anonymousFunction)
            }
        }

    override fun visitPropertyAccessor(propertyAccessor: FirPropertyAccessor, data: Context): Nothing? =
        extendAndCheck(propertyAccessor, data) {
            context(reporter) {
                propertyAccessor.valueParameters.forEach {
                    resolveService.resolveReturnTypePermissions(it)
                }
            }
        }

    // Expressions

    override fun visitFunctionCall(functionCall: FirFunctionCall, data: Context): Nothing? =
        analyze(functionCall, data) {
            checkAccessExpression(functionCall, data)
        }

    override fun visitDelegatedConstructorCall(
        delegatedConstructorCall: FirDelegatedConstructorCall,
        data: Context
    ): Nothing? = analyze(delegatedConstructorCall, data) {
        val calleeReference = delegatedConstructorCall.calleeReference
        if (
            calleeReference is FirResolvedNamedReference &&
            (calleeReference.resolvedSymbol as FirConstructorSymbol).callableId != ICNames.ENUM_CONSTRUCTOR
        ) {
            checkAccessExpression(delegatedConstructorCall, data)
        }
    }

    override fun visitPropertyAccessExpression(
        propertyAccessExpression: FirPropertyAccessExpression,
        data: Context
    ): Nothing? = analyze(propertyAccessExpression, data) {
        checkAccessExpression(propertyAccessExpression, data)
    }

    @OptIn(SymbolInternals::class)
    override fun visitResolvedQualifier(resolvedQualifier: FirResolvedQualifier, data: Context): Nothing? =
        analyze(resolvedQualifier, data) {
            val symbol = resolvedQualifier.symbol ?: return@analyze
            checkAccessPermissions(
                resolvedQualifier,
                symbol.name.asString(),
                context(reporter) {
                    resolveService.getDeclarationAnnotatedPermissions(symbol.fir)
                },
                data
            )
        }

    override fun visitTypeOperatorCall(typeOperatorCall: FirTypeOperatorCall, data: Context): Nothing? =
        analyze(typeOperatorCall, data) {
            when (typeOperatorCall.operation) {
                FirOperation.AS, FirOperation.SAFE_AS -> {
                    val targetTypeRef = typeOperatorCall.conversionTypeRef as? FirResolvedTypeRef ?: return@analyze
                    val argument = typeOperatorCall.argument
                    context(reporter) {
                        resolveService.resolveReturnTypePermissions(argument)
                    }
                    val state = session.typeContext.newTypeCheckerState(
                        errorTypesEqualToAnything = false,
                        stubTypesEqualToAnything = false
                    )
                    val result = context(state, state.typeSystemContext) {
                        checkIsSubtype(
                            context(reporter) {
                                resolveService.transformAnnotatedTypes(targetTypeRef.coneType)
                            },
                            targetTypeRef.source,
                            argument.resolvedType,
                            argument.source,
                            null
                        )
                    }
                    when (result) {
                        is FunctionTypeCheckResult.Leak -> context(dContext) {
                            reporter.reportOn(
                                typeOperatorCall.source,
                                ICDiagnostics.KIC_LEAKY_CAST,
                                result.leakedPermissions
                            )
                        }

                        is FunctionTypeCheckResult.Poison -> context(dContext) {
                            reporter.reportOn(result.source, ICDiagnostics.KIC_POISON_FUNCTION_TYPE)
                        }

                        else -> {}
                    }
                }

                else -> {}
            }
        }

    override fun visitCallableReferenceAccess(
        callableReferenceAccess: FirCallableReferenceAccess,
        data: Context
    ): Nothing? = analyze(callableReferenceAccess, data) {
        callableReferenceAccess.replaceConeTypeOrNull(
            context(reporter) {
                resolveService.resolveCallableReferenceType(
                    callableReferenceAccess.resolvedType,
                    callableReferenceAccess.calleeReference,
                    callableReferenceAccess.dispatchReceiver
                )
            }
        )
    }

    override fun visitSamConversionExpression(
        samConversionExpression: FirSamConversionExpression,
        data: Context
    ): Nothing? = analyze(samConversionExpression, data) {
        val samFunctionType = context(reporter) {
            resolveService.resolveSamFunctionType(samConversionExpression.resolvedType) ?: return@analyze
        }
        val expression = samConversionExpression.expression
        context(reporter) {
            resolveService.resolveReturnTypePermissions(expression)
        }
        val state = session.typeContext.newTypeCheckerState(
            errorTypesEqualToAnything = false,
            stubTypesEqualToAnything = false
        )
        val result = context(state, state.typeSystemContext) {
            checkIsSubtype(
                samFunctionType,
                samConversionExpression.source,
                expression.resolvedType,
                expression.source,
                null
            )
        }
        when (result) {
            is FunctionTypeCheckResult.Leak -> context(dContext) {
                reporter.reportOn(
                    samConversionExpression.source,
                    ICDiagnostics.KIC_LEAKY_ASSIGNMENT,
                    result.leakedPermissions
                )
            }

            is FunctionTypeCheckResult.Poison -> context(dContext) {
                reporter.reportOn(result.source, ICDiagnostics.KIC_POISON_FUNCTION_TYPE)
            }

            else -> {}
        }
    }

    // Other checks

    override fun visitAnnotationCall(annotationCall: FirAnnotationCall, data: Context): Nothing? =
        analyze(annotationCall, data) {
            when (annotationCall.toAnnotationClassIdSafe(session)) {
                ICNames.ANNOT_RESTRICT_ANNOTATION, ICNames.ANNOT_RESTRICT, ICNames.ANNOT_UNCHECKED -> {
                    if (
                        annotationCall.findArgumentByName(ICNames.ARG_PERMISSIONS)?.unwrapVarargValue().isNullOrEmpty()
                    ) {
                        context(dContext) {
                            reporter.reportOn(annotationCall.source, ICDiagnostics.KIC_EMPTY_ANNOTATION)
                        }
                    }
                }
            }
        }
}
