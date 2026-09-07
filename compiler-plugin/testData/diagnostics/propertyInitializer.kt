// RUN_PIPELINE_TILL: FRONTEND

package foo.bar

import st.evening.kt.invokecontrol.ICRestrictAnnotation

@ICRestrictAnnotation("enclave")
annotation class Enclave

@Enclave
fun ok(): String = "OK"

@Enclave
val initializer: String = <!KIC_INSUFFICIENT_PERMISSIONS!>ok()<!>

@Enclave
fun okLazy(): Lazy<String> = lazy { "OK" }

@Enclave
val delegate: String by <!KIC_INSUFFICIENT_PERMISSIONS!>okLazy()<!>
