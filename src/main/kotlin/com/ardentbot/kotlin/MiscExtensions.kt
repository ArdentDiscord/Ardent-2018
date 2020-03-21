package com.ardentbot.kotlin

import com.ardentbot.core.Sender
import java.lang.management.ManagementFactory
import java.util.concurrent.TimeUnit
import javax.management.Attribute
import javax.management.ObjectName

fun after(time: Int, consumer: () -> Unit, unit: TimeUnit = TimeUnit.SECONDS) {
    Sender.scheduledExecutor.schedule({ consumer.invoke() }, time.toLong(), unit)
}

/**
 * Full credit goes to http://stackoverflow.com/questions/18489273/how-to-getWithIndex-percentage-of-cpu-usage-of-os-from-java
 */
fun getProcessCpuLoad(): Double {
    val mbs = ManagementFactory.getPlatformMBeanServer()
    val name = ObjectName.getInstance("java.lang:type=OperatingSystem")
    val list = mbs.getAttributes(name, arrayOf("ProcessCpuLoad"))

    if (list.isEmpty()) return java.lang.Double.NaN

    val att = list[0] as Attribute
    val value = att.value as Double

    // usually takes a couple of seconds before we getWithIndex real values
    if (value == -1.0) return Double.NaN
    // returns a percentage value with 1 decimal point precision
    return (value * 1000).toInt() / 10.0
}