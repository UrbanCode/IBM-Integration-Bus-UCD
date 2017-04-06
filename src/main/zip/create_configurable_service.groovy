/*
* Licensed Materials - Property of IBM Corp.
* IBM UrbanCode Build
* IBM UrbanCode Deploy
* IBM UrbanCode Release
* IBM AnthillPro
* (c) Copyright IBM Corporation 2002, 2017. All Rights Reserved.
*
* U.S. Government Users Restricted Rights - Use, duplication or disclosure restricted by
* GSA ADP Schedule Contract with IBM Corp.
*/
import com.urbancode.air.plugin.AirPluginTool;
import com.urbancode.air.plugin.wmbcmp.IIBHelper;

File workDir = new File('.');

AirPluginTool apTool = new AirPluginTool(this.args[0], this.args[1])
def props = apTool.getStepProperties(System.getenv("UCD_SECRET_VAR"))
def servName = props['servName'];
def servType = props['servType'];
def propsString = props['props'];
def properties = new HashMap<String,String>();
def deleteMissing = Boolean.valueOf(props['deleteMissing']);
propsString.split('\n').each {
    def parts = it.split('->|=', 2);
    if (parts.length != 2) {
        System.out.println("Found a property definition that doesn't match the expected syntax. Skipping.");
        System.out.println(it);
    }
    else {
       properties.put(parts[0].trim(), parts[1].trim());
    }
}

def helper = new IIBHelper(props)

try {
    helper.createOrUpdateConfigurableService(servType, servName, properties, deleteMissing);
}
catch (Exception e) {
    e.printStackTrace();
    throw e;
}
finally {
    helper.cleanUp();
}
