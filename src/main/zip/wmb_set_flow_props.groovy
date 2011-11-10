import com.urbancode.air.plugin.wmbcmp.*;
import com.urbancode.air.plugin.*;

File workDir = new File('.');

Properties props = PropertiesHelper.getProperties(new File(args[0]));
WMBHelper helper = new WMBHelper(props);

try { 
    props['msgFlows'].split('\n').each { msgFlow ->
        println "Setting properties on message flow ${msgFlow}";
        props['properties'].split('\n').each { property ->
            String name = property.split('=',2)[0];
            String value = property.split('=',2)[1];
            helper.setMsgFlowProperty(msgFlow, name, value);
        }
    }
    
    props['msgFlows'].split('\n').each { msgFlow ->
        helper.deployMsgFlowConfig(msgFlow);
    }
}
catch (Exception e) {
    e.printStackTrace();
    throw e;
}
