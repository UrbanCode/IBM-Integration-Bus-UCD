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
def helper = new IIBHelper(props)

try {
    props['properties'].split('\n').each { property ->
        if (!property.matches(".*:.*=.*")) {
            throw new IllegalStateException("Must specify a component type followed by a colon, and"
                + " a property name and value in the format 'type:name=value'.")
        }

        property = property.split(':', 2)

        String propType = property[0].trim().toLowerCase()
        String name = property[1].split('=', 2)[0]; // object name and property name 'object/property'
        String value = property[1].split('=', 2)[1];

        if (!propType || !name) {
            throw new IllegalStateException("The component type and property name for this property definition"
                + " must not be empty, and must be in the format 'type:name=value'.");
        }

        helper.setBrokerProperty(name, value, propType);
    }
}
catch (Exception e) {
    e.printStackTrace();
    throw e;
}
finally {
    helper.cleanUp();
}