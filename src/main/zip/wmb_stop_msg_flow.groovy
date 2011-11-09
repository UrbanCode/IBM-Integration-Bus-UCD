import com.urbancode.air.plugin.wmbcmp.*;
import com.urbancode.air.plugin.*;

File workDir = new File('.');

Properties props = PropertiesHelper.getProperties(new File(args[0]));
WMBHelper helper = new WMBHelper(props);
def messageFlows = props['messageFlows']

try {
    if (messageFlows) {
        for (def messageFlow in messageFlows.split("\n")) {
            println "Stopping message flow ${messageFlow}";
            helper.stopMsgFlow(messageFlow.trim());
        }
    }
    else {
        println "Stopping all message flows!"
        helper.stopAllMsgFlows();
    }
}
catch (Exception e) {
    e.printStackTrace();
    throw e;
}