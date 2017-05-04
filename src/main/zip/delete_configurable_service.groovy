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

def servName = props['servName'];
def servType = props['servType'];

def helper = new IIBHelper(props)

try {
    helper.deleteConfigurableService(servType,servName);
}
catch (Exception e) {
    e.printStackTrace();
    throw e;
}
finally {
    helper.cleanUp();
}
