/**
 * (c) Copyright IBM Corporation 2013, 2017.
 * This is licensed under the following license.
 * The Eclipse Public 1.0 License (http://www.eclipse.org/legal/epl-v10.html)
 * U.S. Government Users Restricted Rights:  Use, duplication or disclosure restricted by GSA ADP Schedule Contract with IBM Corp.
 */
import com.urbancode.air.AirPluginTool;
import com.urbancode.air.plugin.wmbcmp.IIBHelper;
import com.ibm.broker.config.proxy.CompletionCodeType;


File workDir = new File('.')

AirPluginTool apTool = new AirPluginTool(this.args[0], this.args[1])
def props = apTool.getStepProperties(System.getenv("UCD_SECRET_VAR"))
def helper = new IIBHelper(props)

String executionGroup = props['executionGroup']
def executionGroups
if(executionGroup != null && !executionGroup.trim().isEmpty()) {
    executionGroups = executionGroup.split('\n|,')*.trim()
    executionGroups -= [null, ""] // remove empty and null entries
}

try {
    props['barFileNames'].split('\n|,')*.trim().each { barFileName ->
        println "${helper.getTimestamp()} Deploying bar file ${barFileName}";

        for (String groupName : executionGroups) {
            helper.setExecutionGroup(groupName);

            if (Boolean.valueOf(props['startStopMsgFlows'])) {
                println "${helper.getTimestamp()} Stopping all Msg Flows"
                helper.stopAllMsgFlows();
            }
            CompletionCodeType completionCode = helper.deployBrokerArchive(barFileName, groupName);
            apTool.setOutputProperty("completionCode", completionCode.toString())

            if (completionCode != CompletionCodeType.success) {
                println("${helper.getTimestamp()} The broker has returned an unsuccessful deployment result.")
                String message = ""

                if (completionCode == CompletionCodeType.failure) {
                    message = "The deployment operation has failed."
                }
                else if (completionCode == CompletionCodeType.cancelled) {
                    message = "The deployment was submitted to the broker, but was cancelled by user action before processing."
                }
                else if (completionCode == CompletionCodeType.pending) {
                    message = "The deployment is queued and waiting to be processed by the broker."
                }
                else if (completionCode == CompletionCodeType.submitted) {
                    message = "The deployment request was sent to the broker's administration agent and is currently being processed."
                }
                else if (completionCode == CompletionCodeType.unknown) {
                    message = "No information has been received from the broker about the deployment request."
                }
                else if (completionCode == CompletionCodeType.initiated) {
                    message = "The deployment has been created and is about to be queued on the broker."
                }
                else if (completionCode == CompletionCodeType.timedOut) {
                    message = "The deployment request was sent to the broker but no response was received in the expected time frame."
                }
                else {
                    throw new Exception("Deployment Result could not be obtained.")
                }
                throw new Exception("Failed deploying bar File ${barFileName} to execution group ${groupName}"
                    + " with completion code : ${completionCode.toString()}: ${message}")
            }
            else {
                println("${helper.getTimestamp()} ${barFileName} was successfully deployed to ${groupName}.")
            }
        }
    }

    if (Boolean.valueOf(props['startStopMsgFlows'])) {
        println "${helper.getTimestamp()} Starting all Msg Flows"
        helper.startAllMsgFlows();
    }
}
catch (Exception e) {
    e.printStackTrace();
    throw e;
}
finally {
    apTool.setOutputProperties(System.getenv("UCD_SECRET_VAR"))
    helper.cleanUp();
}
