/**
 * (c) Copyright IBM Corporation 2017.
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

String executionGroup = props['executionGroup'].trim()
boolean failFast = Boolean.parseBoolean(props['failFast']);

String exists = "fail"
try {
    System.out.println("[Action] Identifying Execution Group: " + executionGroup)
    if (helper.getExecutionGroup(executionGroup)) {
        exists = "true"
        System.out.println("[Info] Execution Group found.")
    } else {
        exists = "false"
        System.out.println("[Info] Execution Group not found.")
    }
}
catch (Exception e) {
    e.printStackTrace()
    throw e
}
finally {
    apTool.setOutputProperty("exists", exists)
    apTool.setOutputProperties(System.getenv("UCD_SECRET_VAR"))
    helper.cleanUp()
}

if (failFast && exists != "true") {
    System.exit(1)
}
