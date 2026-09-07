package st.evening.kt.invokecontrol.kplugin.permission

import org.jetbrains.kotlin.fir.containingClassLookupTag
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.renderReadable

sealed interface PermissionSource {
    fun getSourceName(): String

    data class RestrictedType(val type: ConeKotlinType) : PermissionSource {
        override fun getSourceName(): String = type.renderReadable()
    }

    data class Class(val symbol: FirClassLikeSymbol<*>) : PermissionSource {
        override fun getSourceName(): String = symbol.name.asStringStripSpecialMarkers()
    }

    data class Function(val symbol: FirFunctionSymbol<*>) : PermissionSource {
        override fun getSourceName(): String = symbol.containingClassLookupTag()?.let {
            "${it.name.asStringStripSpecialMarkers()}.${symbol.name.asStringStripSpecialMarkers()}()"
        } ?: "${symbol.name.asStringStripSpecialMarkers()}()"
    }

    data class Property(val symbol: FirPropertySymbol) : PermissionSource {
        override fun getSourceName(): String = symbol.containingClassLookupTag()?.let {
            "${it.name.asStringStripSpecialMarkers()}.${symbol.name.asStringStripSpecialMarkers()}"
        } ?: symbol.name.asStringStripSpecialMarkers()
    }
}
