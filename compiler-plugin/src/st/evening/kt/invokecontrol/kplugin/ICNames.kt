package st.evening.kt.invokecontrol.kplugin

import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

object ICNames {
    val ENUM_CONSTRUCTOR: CallableId = CallableId(
        ClassId(FqName("kotlin"), Name.identifier("Enum")), Name.identifier("Enum")
    )

    val PACKAGE: FqName = FqName("st.evening.kt.invokecontrol")
    val ANNOT_CONSTANT: ClassId = ClassId(PACKAGE, Name.identifier("ICConstant"))
    val ANNOT_RESTRICT: ClassId = ClassId(PACKAGE, Name.identifier("ICRestrict"))
    val ANNOT_RESTRICT_ANNOTATION: ClassId = ClassId(PACKAGE, Name.identifier("ICRestrictAnnotation"))
    val ANNOT_UNCHECKED: ClassId = ClassId(PACKAGE, Name.identifier("ICUnchecked"))
    val ARG_KEY: Name = Name.identifier("key")
    val ARG_PERMISSIONS: Name = Name.identifier("permissions")
}
