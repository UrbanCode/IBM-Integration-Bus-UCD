import com.urbancode.air.plugin.wmbcmp.*;
import com.urbancode.air.plugin.*;

File workDir = new File('.');

Properties props = PropertiesHelper.getProperties(new File(args[0]));
WMBHelper helper = new WMBHelper(props);
def messageFlows = props['messageFlows']

try {
    if (messageFlows) {
        for (def messageFlow in messageFlows.split("\n")) {
            println "Starting message flow ${messageFlow}";
            helper.startMsgFlow(messageFlow.trim());
        }
    }
    else {
        println "Starting all message flows!"
        helper.startAllMsgFlows();
    }
}
catch (Exception e) {
    e.printStackTrace();
    throw e;
}