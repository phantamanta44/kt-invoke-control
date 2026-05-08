package st.evening.kt.invokecontrol

import org.jetbrains.kotlin.generators.dsl.junit5.generateTestGroupSuiteWithJUnit5
import st.evening.kt.invokecontrol.runners.AbstractJvmBoxTest
import st.evening.kt.invokecontrol.runners.AbstractJvmDiagnosticTest

fun main() {
    generateTestGroupSuiteWithJUnit5 {
        testGroup(testDataRoot = "compiler-plugin/testData", testsRoot = "compiler-plugin/test-gen") {
            testClass<AbstractJvmDiagnosticTest> {
                model("diagnostics")
            }

            testClass<AbstractJvmBoxTest> {
                model("box")
            }

//            testClass<AbstractJsBoxTest> {
//                model("box")
//            }
        }
    }
}
