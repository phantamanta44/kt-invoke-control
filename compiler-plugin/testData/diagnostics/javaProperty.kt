// RUN_PIPELINE_TILL: FRONTEND
// IC_RESTRICT_ANNOTATION: kotlin/Deprecated=deprecated

@file:Suppress("DEPRECATION")

package foo.bar

import java.net.HttpURLConnection
import java.util.Date

fun test1() {
    val x = <!KIC_INSUFFICIENT_PERMISSIONS!>Date(0).year<!>
    <!KIC_INSUFFICIENT_PERMISSIONS!>Date(0).year<!> = 2052
    val y = <!KIC_INSUFFICIENT_PERMISSIONS!>HttpURLConnection.HTTP_SERVER_ERROR<!>
}

class Test2 : SecurityManager() {
    fun test2() {
        val x = <!KIC_INSUFFICIENT_PERMISSIONS!>inCheck<!>
    }
}
