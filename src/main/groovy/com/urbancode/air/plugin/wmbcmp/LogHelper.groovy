/**
 * (c) Copyright IBM Corporation 2013, 2017.
 * This is licensed under the following license.
 * The Eclipse Public 1.0 License (http://www.eclipse.org/legal/epl-v10.html)
 * U.S. Government Users Restricted Rights:  Use, duplication or disclosure restricted by GSA ADP Schedule Contract with IBM Corp.
 */
package com.urbancode.air.plugin.wmbcmp

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.lang.management.ManagementFactory
import java.lang.management.OperatingSystemMXBean

class LogHelper {
    public static String getTimestamp() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("[MM/dd/yyyy HH:mm:ss]")

        return dateFormat.format(new Date())
    }

    public static void outputCpuUsage() {
        NumberFormat percFormat = NumberFormat.getPercentInstance()
        percFormat.setMinimumFractionDigits(5)
        OperatingSystemMXBean osBean = ManagementFactory.getPlatformMXBean(
                        OperatingSystemMXBean.class)
        // What % CPU load this current JVM is taking, from 0.0-1.0
        def jvmUsage = osBean.getProcessCpuLoad()

        // What % load the overall system is at, from 0.0-1.0
        def systemUsage = osBean.getSystemCpuLoad()

        if (jvmUsage > 0) {
            println("Percent CPU load of current JVM: " + percFormat.format(jvmUsage))
        }
        else {
            println("Percent CPU load of current JVM is unavailable.")
        }

        if (systemUsage > 0) {
            println("Percent CPU load of current JVM: " + percFormat.format(systemUsage))
        }
        else {
            println("Percent CPU load of overall system is unavailable.")
        }
    }
}
