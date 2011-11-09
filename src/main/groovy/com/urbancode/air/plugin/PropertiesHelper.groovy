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
