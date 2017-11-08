/**
 * (c) Copyright IBM Corporation 2013, 2017.
 * This is licensed under the following license.
 * The Eclipse Public 1.0 License (http://www.eclipse.org/legal/epl-v10.html)
 * U.S. Government Users Restricted Rights:  Use, duplication or disclosure restricted by GSA ADP Schedule Contract with IBM Corp.
 */
import com.urbancode.air.AirPluginTool;
import com.urbancode.air.plugin.wmbcmp.IIBHelper;

File workDir = new File('.');

AirPluginTool apTool = new AirPluginTool(this.args[0], this.args[1])
def props = apTool.getStepProperties(System.getenv("UCD_SECRET_VAR"))

def helper = new IIBHelper(props)

def executionGroup = props['executionGroup']

try {
    helper.setExecutionGroup(executionGroup)
    String regex = props['regex']
    boolean deleteSharedLibs = Boolean.valueOf(props['deleteSharedLibs'])
    println "Deleting contents that have been deployed via BAR files matching regex: ${regex}"
    helper.deleteMessageFlowsMatchingRegex(regex, deleteSharedLibs)
}
catch (Exception e) {
    e.printStackTrace()
    throw e
}
finally {
    helper.cleanUp()
}
