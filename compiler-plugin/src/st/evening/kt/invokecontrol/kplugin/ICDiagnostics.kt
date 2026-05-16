package st.evening.kt.invokecontrol.kplugin

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.build.joinToReadableString
import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactoryToRendererMap
import org.jetbrains.kotlin.diagnostics.KtDiagnosticsContainer
import org.jetbrains.kotlin.diagnostics.error0
import org.jetbrains.kotlin.diagnostics.error1
import org.jetbrains.kotlin.diagnostics.error2
import org.jetbrains.kotlin.diagnostics.rendering.BaseDiagnosticRendererFactory
import org.jetbrains.kotlin.diagnostics.rendering.CommonRenderers
import org.jetbrains.kotlin.diagnostics.rendering.DiagnosticParameterRenderer
import org.jetbrains.kotlin.diagnostics.rendering.RenderingContext
import org.jetbrains.kotlin.diagnostics.warning0
import org.jetbrains.kotlin.diagnostics.warning1
import st.evening.kt.invokecontrol.kplugin.permission.Permission

object ICDiagnostics : KtDiagnosticsContainer() {
    val KIC_INVALID_PERMISSION_ARGUMENT_KEY by error1<PsiElement, String>()
    val KIC_NO_SUCH_PERMISSION_ARGUMENT by error1<PsiElement, String>()
    val KIC_INVALID_PERMISSION_ARGUMENT_VALUE by error1<PsiElement, String>()
    val KIC_INSUFFICIENT_PERMISSIONS by error2<PsiElement, Set<Permission>, Set<String>>()
    val KIC_LEAKY_DECLARATION by error2<PsiElement, Set<Permission>, Set<String>>()
    val KIC_LEAKY_ASSIGNMENT by error1<PsiElement, Set<Permission>>()
    val KIC_LEAKY_CAST by warning1<PsiElement, Set<Permission>>()
    val KIC_POISON_FUNCTION_TYPE by error0<PsiElement>()
    val KIC_NOT_CONSTANT by error0<PsiElement>()
    val KIC_OVERRIDE_CONSTANT_MISMATCH by error1<PsiElement, String>()
    val KIC_UNSUPPORTED_DEPENDENT_FUNCTION by error0<PsiElement>()
    val KIC_EMPTY_ANNOTATION by warning0<PsiElement>()
    val KIC_REDUNDANT_UNCHECKED by warning1<PsiElement, Set<Permission>>()

    override fun getRendererFactory(): BaseDiagnosticRendererFactory = Messages

    private object Messages : BaseDiagnosticRendererFactory() {
        override val MAP: KtDiagnosticFactoryToRendererMap by KtDiagnosticFactoryToRendererMap("KIC") { map ->
            map.put(KIC_INVALID_PERMISSION_ARGUMENT_KEY, "Invalid permission argument key {0}", CommonRenderers.STRING)
            map.put(KIC_NO_SUCH_PERMISSION_ARGUMENT, "No such permission argument {0}", CommonRenderers.STRING)
            map.put(
                KIC_INVALID_PERMISSION_ARGUMENT_VALUE,
                "Invalid permission argument value for {0}",
                CommonRenderers.STRING
            )
            map.put(
                KIC_INSUFFICIENT_PERMISSIONS,
                "Missing required permissions {0} from {1}",
                PermissionSetRenderer,
                StringSetRenderer
            )
            map.put(
                KIC_LEAKY_DECLARATION,
                "Declaration leaks permissions {0} from {1}",
                PermissionSetRenderer,
                StringSetRenderer
            )
            map.put(
                KIC_LEAKY_ASSIGNMENT,
                "Restricted function type leaks permissions {0}",
                PermissionSetRenderer
            )
            map.put(
                KIC_LEAKY_CAST,
                "Cast to restricted function type may leak permissions {0}",
                PermissionSetRenderer
            )
            map.put(KIC_POISON_FUNCTION_TYPE, "Conflicting permission sets")
            map.put(KIC_NOT_CONSTANT, "Non-constant value for constant parameter")
            map.put(
                KIC_OVERRIDE_CONSTANT_MISMATCH,
                "Mismatched override for constant parameter with key {0}",
                CommonRenderers.STRING
            )
            map.put(KIC_UNSUPPORTED_DEPENDENT_FUNCTION, "Dependent function objects are not supported")
            map.put(KIC_EMPTY_ANNOTATION, "Empty permission annotation")
            map.put(KIC_REDUNDANT_UNCHECKED, "Redundant unchecked permissions {0}", PermissionSetRenderer)
        }
    }
}

object PermissionSetRenderer : DiagnosticParameterRenderer<Set<Permission>> {
    override fun render(obj: Set<Permission>, renderingContext: RenderingContext): String {
        val strings = obj.mapTo(mutableListOf()) { it.toString() }
        strings.sort()
        return "[${strings.joinToReadableString()}]"
    }
}

object StringSetRenderer : DiagnosticParameterRenderer<Set<String>> {
    override fun render(obj: Set<String>, renderingContext: RenderingContext): String =
        "[${obj.sorted().joinToReadableString()}]"
}
