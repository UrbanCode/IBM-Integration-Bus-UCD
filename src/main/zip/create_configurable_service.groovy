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
import com.urbancode.air.plugin.wmbcmp.*;
import com.urbancode.air.plugin.*;

File workDir = new File('.');

Properties props = PropertiesHelper.getProperties(new File(args[0]));
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
