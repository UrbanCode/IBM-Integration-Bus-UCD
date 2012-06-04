import com.urbancode.air.plugin.wmbcmp.*;
import com.urbancode.air.plugin.*;

File workDir = new File('.');

Properties props = PropertiesHelper.getProperties(new File(args[0]));
WMBHelper helper = new WMBHelper(props);

try { 
	String regex = props['regex'];
	println "Deleting message flows that have been deployed via BAR files matching regex: ${regex}";
	helper.deleteMessageFlowsMatchingRegex(regex);
    
    helper.cleanUp();
}
catch (Exception e) {
    e.printStackTrace();
    throw e;
}
