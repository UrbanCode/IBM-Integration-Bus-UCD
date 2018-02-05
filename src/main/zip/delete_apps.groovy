/**
 * (c) Copyright IBM Corporation 2018.
 * This is licensed under the following license.
 * The Eclipse Public 1.0 License (http://www.eclipse.org/legal/epl-v10.html)
 * U.S. Government Users Restricted Rights:  Use, duplication or disclosure restricted by GSA ADP Schedule Contract with IBM Corp.
 */
import com.urbancode.air.AirPluginTool;
import com.urbancode.air.plugin.wmbcmp.IIBHelper;

File workDir = new File('.')

AirPluginTool apTool = new AirPluginTool(this.args[0], this.args[1])
def props = apTool.getStepProperties(System.getenv("UCD_SECRET_VAR"))

def helper = new IIBHelper(props)

def appNames = props['appNames'].split("\n|,")*.trim()
def failFast = Boolean.valueOf(props['failFast'])
String executionGroup = props['executionGroup'].trim()
int timeout = Integer.valueOf(props['timeout']?.trim()?:-1)

try {
    helper.setExecutionGroup(executionGroup)
    helper.deleteApplications(appNames, executionGroup, timeout, failFast)
}
finally {
    helper.cleanUp()
}