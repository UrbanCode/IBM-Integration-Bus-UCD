/**
 * © Copyright IBM Corporation 2016, 2017.
 * This is licensed under the following license.
 * The Eclipse Public 1.0 License (http://www.eclipse.org/legal/epl-v10.html)
 * U.S. Government Users Restricted Rights:  Use, duplication or disclosure restricted by GSA ADP Schedule Contract with IBM Corp.
 */
import com.urbancode.air.AirPluginTool
import com.urbancode.air.ExitCodeException
import com.urbancode.air.plugin.wmbcmp.MQSIHelper

MQSIHelper mqHelper
AirPluginTool apTool = new AirPluginTool(this.args[0], this.args[1])
def props = apTool.getStepProperties()

def installDir = props['installDir'].trim()
def integrationNode = props['integrationNode'].trim()
def queueManager = props['queueManager'].trim()
def serviceUser = props['serviceUserId'].trim()
def servicePass = props['servicePassword']
def additionalArgs = props['additionalArgs'].trim()

try {
    mqHelper = new MQSIHelper(installDir, apTool.isWindows)
}
catch(FileNotFoundException ex) {
    println("Unable to locate mqsi script directory:")
    println(ex.message)
    System.exit(1)
}

try {
    mqHelper.createBroker(integrationNode, queueManager, serviceUser, servicePass, additionalArgs)
}
catch(ExitCodeException ex) {
    println("Could not successfully create broker:")
    println(ex.message)
    System.exit(1)
}
catch(FileNotFoundException ex) {
    println("Couldn't locate necessary mqsi script:")
    println(ex.message)
    System.exit(1)
}
