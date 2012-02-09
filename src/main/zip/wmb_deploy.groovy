import com.urbancode.air.plugin.wmbcmp.*;
import com.urbancode.air.plugin.*;

File workDir = new File('.');

Properties props = PropertiesHelper.getProperties(new File(args[0]));
WMBHelper helper = new WMBHelper(props);

try { 
    if (Boolean.valueOf(props['startStopMsgFlows'])) {
        println "Stopping all Msg Flows"
        helper.stopAllMsgFlows();
    }

    props['barFileNames'].split('\n').each { barFileName ->
        println "Deploying bar file ${barFileName}";
        helper.deployBrokerArchive(barFileName);
    }

    if (Boolean.valueOf(props['startStopMsgFlows'])) {
        println "Starting all Msg Flows"
        helper.startAllMsgFlows();
    }

    helper.cleanUp();
}
catch (Exception e) {
    e.printStackTrace();
    throw e;
}
