// RUN_PIPELINE_TILL: FRONTEND

package foo.bar

import st.evening.kt.invokecontrol.ICRestrict

@ICRestrict("property")
@get:ICRestrict("getter")
@set:ICRestrict("setter")
var value = "ERROR"

fun none() {
    <!KIC_INSUFFICIENT_PERMISSIONS!>value<!> = "OK"
    val x = <!KIC_INSUFFICIENT_PERMISSIONS!>value<!>
}

@ICRestrict("property")
fun p() {
    <!KIC_INSUFFICIENT_PERMISSIONS!>value<!> = "OK"
    val x = <!KIC_INSUFFICIENT_PERMISSIONS!>value<!>
}

@ICRestrict("setter")
fun s() {
    <!KIC_INSUFFICIENT_PERMISSIONS!>value<!> = "OK"
    val x = <!KIC_INSUFFICIENT_PERMISSIONS!>value<!>
}

@ICRestrict("getter")
fun g() {
    <!KIC_INSUFFICIENT_PERMISSIONS!>value<!> = "OK"
    val x = <!KIC_INSUFFICIENT_PERMISSIONS!>value<!>
}

@ICRestrict("setter", "getter")
fun sg() {
    <!KIC_INSUFFICIENT_PERMISSIONS!>value<!> = "OK"
    val x = <!KIC_INSUFFICIENT_PERMISSIONS!>value<!>
}

@ICRestrict("property", "getter")
fun pg() {
    <!KIC_INSUFFICIENT_PERMISSIONS!>value<!> = "OK"
    val x = value
}

@ICRestrict("property", "setter")
fun ps() {
    value = "OK"
    val x = <!KIC_INSUFFICIENT_PERMISSIONS!>value<!>
}

@ICRestrict("property", "setter", "getter")
fun psg() {
    value = "OK"
    val x = value
}
