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
package com.urbancode.air.plugin;

public class PropertiesHelper {

    public static Properties getProperties(File propsFile) {
        final def props = new Properties();
        InputStream inputPropsStream;
        try {
            inputPropsStream = new FileInputStream(propsFile);
            props.load(inputPropsStream);
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
        finally {
            if (inputPropsStream != null) {
                inputPropsStream.close();
            }
        }
        
        return props;
    }
}
