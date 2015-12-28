/*
* Licensed Materials - Property of IBM Corp.
* IBM UrbanCode Build
* IBM UrbanCode Deploy
* IBM UrbanCode Release
* IBM AnthillPro
* (c) Copyright IBM Corporation 2002, 2013. All Rights Reserved.
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

def helper = new IIBHelper(props)

try {
    helper.deleteConfigurableService(servType,servName);
    helper.deployBrokerConfig();
}
catch (Exception e) {
    e.printStackTrace();
    throw e;
}
finally {
    helper.cleanUp();
}