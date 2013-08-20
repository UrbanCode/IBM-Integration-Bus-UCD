package com.urbancode.air.plugin.wmbcmp;

import com.ibm.broker.config.proxy.*;
import java.util.Properties;
import java.util.Enumeration;
import java.util.Set;
import java.util.HashSet;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class WMBHelper {

    String host;
    int port;
    String channel;
    String queueManager;
    String executionGroup;
    BrokerConnectionParameters bcp;
    BrokerProxy brokerProxy;
    ExecutionGroupProxy executionGroupProxy;
    boolean isIncremental = true;
    def logProxy;
    Date startTime;
    def timeout;
    boolean isDebugEnabled = false;

    public WMBHelper(Properties props) {
        if (props['username']?.trim()) {
            System.out.println("Setting user.name to " + props['username']);
            System.setProperty("user.name", props['username']);
        }
        host = props['brokerHost'];
        port = Integer.valueOf(props['port']);
        channel = props['channel']?.trim();
        queueManager = props['queueManager'];
        executionGroup = props['executionGroup'];
        timeout = Long.valueOf(props['timeout']?.trim()?:60000);

        bcp = new MQBrokerConnectionParameters(host, port, queueManager);

        if (channel) {
           bcp.setAdvancedConnectionParameters(channel, null,null, -1, -1, null);
        }

        if (props['debugFile']) {
            isDebugEnabled = true;
            BrokerProxy.enableAdministrationAPITracing(props['debugFile']);
        }

        brokerProxy = BrokerProxy.getInstance(bcp);
        if (executionGroup != null && executionGroup.trim() != "") {
            executionGroupProxy = brokerProxy.getExecutionGroupByName(executionGroup);
        }

        if (Boolean.valueOf(props['fullDeploy'])) {
            isIncremental = false;
        }
        
        startTime = new Date(System.currentTimeMillis());
        logProxy = brokerProxy.getLog();
    }
   
    public void stopAllMsgFlows() {
        if (brokerProxy == null || bcp == null) {
            throw new IllegalStateException("Broker Proxy is uninitilized!");
        }

        if (executionGroupProxy == null) {
            throw new IllegalStateException("Execution group proxy is null! Make sure it is configured correctly!");
        }
   
        executionGroupProxy.stopMessageFlows();
    }

    public void startAllMsgFlows() {
        if (brokerProxy == null || bcp == null) {
            throw new IllegalStateException("Broker Proxy is uninitilized!");
        }

        if (executionGroupProxy == null) {
            throw new IllegalStateException("Execution group proxy is null! Make sure it is configured correctly!");
        }

        executionGroupProxy.startMessageFlows();
    }

    public void deployBrokerArchive(String fileName) {
        if (brokerProxy == null || bcp == null) {
            throw new IllegalStateException("Broker Proxy is uninitilized!");
        }

        if (executionGroupProxy == null) {
            throw new IllegalStateException("Execution group proxy is null! Make sure it is configured correctly!");
        }
        
        println "Using timeout ${timeout}";
        DeployResult dr = executionGroupProxy.deploy(fileName, isIncremental, timeout);
        
        if (dr.getCompletionCode() != CompletionCodeType.success) {
            checkDeployResult(dr);
            String code = "unknown";
            if (dr.getCompletionCode() == CompletionCodeType.failure) {
                code = "failure";
            }
            else if (dr.getCompletionCode() == CompletionCodeType.cancelled) {
                code = "cancelled";
            }
            else if (dr.getCompletionCode() == CompletionCodeType.pending) {
                code = "pending";
            }
            else if (dr.getCompletionCode() == CompletionCodeType.submitted) {
                code = "submitted";
            }
            throw new Exception("Failed deploying bar File ${fileName} with completion code : " + code);
        }

        checkDeployResult(dr);
    }
    
    public String[] getMessageFlowsFromProperties(props) {
        //todo
        
    }
    
    public String[] getMessageFlowsFromBarFile(String fileName) {
        //todo
        
    }
    
    public void startMsgFlow(String msgFlowName) {
        if (brokerProxy == null || bcp == null) {
            throw new IllegalStateException("Broker Proxy is uninitilized!");
        }

        if (executionGroupProxy == null) {
            throw new IllegalStateException("Execution group proxy is null! Make sure it is configured correctly!");
        }
        
        MessageFlowProxy msgFlowProxy = executionGroupProxy.getMessageFlowByName(msgFlowName);
        if ( msgFlowProxy == null ) {
            throw new Exception("could not get message flow to start!");
        }
        msgFlowProxy.start();
    }
    
    public void stopMsgFlow(String msgFlowName) {
        if (brokerProxy == null || bcp == null) {
            throw new IllegalStateException("Broker Proxy is uninitilized!");
        }

        if (executionGroupProxy == null) {
            throw new IllegalStateException("Execution group proxy is null! Make sure it is configured correctly!");
        }
        
        MessageFlowProxy msgFlowProxy = executionGroupProxy.getMessageFlowByName(msgFlowName);
        if ( msgFlowProxy == null ) {
            throw new Exception("could not get message flow to start!");
        }
        msgFlowProxy.stop();
        
    }
    
    public void setBrokerProperty(String name, String value) {
        if (brokerProxy == null || bcp == null) {
            throw new IllegalStateException("Broker Proxy is uninitilized!");
        }
        
        String oldVal = brokerProxy.getRuntimeProperty(name);
        println "Setting property ${name} to ${value} from ${oldVal} on broker!";
        brokerProxy.setRuntimeProperty(name, value);
    }
    
    public void setExecutionGroupProperty(String name, String value) {
        if (brokerProxy == null || bcp == null) {
            throw new IllegalStateException("Broker Proxy is uninitilized!");
        }
        
        if (executionGroupProxy == null) {
            throw new IllegalStateException("Execution group proxy is null! Make sure it is configured correctly!");
        }
        
        String oldVal = executionGroupProxy.getRuntimeProperty(name);
        println "Setting property ${name} to ${value} from ${oldVal} on Execution Group ${executionGroup}!";
        executionGroupProxy.setRuntimeProperty(name, value);
    }
    
    public void setMsgFlowProperty(String msgFlowName, String name, String value) {
        if (brokerProxy == null || bcp == null) {
            throw new IllegalStateException("Broker Proxy is uninitilized!");
        }
        
        if (executionGroupProxy == null) {
            throw new IllegalStateException("Execution group proxy is null! Make sure it is configured correctly!");
        }
        
        MessageFlowProxy msgFlowProxy = executionGroupProxy.getMessageFlowByName(msgFlowName);
        if ( msgFlowProxy == null ) {
            throw new Exception("could not get message flow to set property on!");
        }
        
        String oldVal = msgFlowProxy.getRuntimeProperty(name);
        println "Setting property ${name} to ${value} from ${oldVal} on Message Flow ${msgFlowName} in Execution Group ${executionGroup}!";
        msgFlowProxy.setRuntimeProperty(name, value);
    }
    
    public void deployBrokerConfig() {
        if (brokerProxy == null || bcp == null) {
            throw new IllegalStateException("Broker Proxy is uninitilized!");
        }
        brokerProxy.deploy(60000);
        checkDeployResult();
    }
    
    public void deployExecutionGroupConfig() {
        if (brokerProxy == null || bcp == null) {
            throw new IllegalStateException("Broker Proxy is uninitilized!");
        }
        
        if (executionGroupProxy == null) {
            throw new IllegalStateException("Execution group proxy is null! Make sure it is configured correctly!");
        }
        
        brokerProxy.deploy(60000);
        checkDeployResult();
    }
    
	public void deleteMessageFlowsMatchingRegex(String regex) {
		if (brokerProxy == null || bcp == null) {
            throw new IllegalStateException("Broker Proxy is uninitilized!");
        }
        
        if (executionGroupProxy == null) {
            throw new IllegalStateException("Execution group proxy is null! Make sure it is configured correctly!");
        }
		
		// Compile the regex 
		Pattern p = Pattern.compile(regex);
		
		// List the flow names from the current BAR file
		Set<DeployedObject> flowsToDelete = new HashSet<DeployedObject>();
		
        // Get an unfiltered enumeration of all message flows in this execution group
        Enumeration<DeployedObject> allDeployedObjectsInThisEG = executionGroupProxy.getDeployedObjects();
        while (allDeployedObjectsInThisEG.hasMoreElements()) {
            DeployedObject thisDeployedObject = allDeployedObjectsInThisEG.nextElement();
            String barFileUsed = thisDeployedObject.getBARFileName();
            System.out.print(thisDeployedObject.getFullName() +" was deployed with BAR file ");
            System.out.print(barFileUsed);
            if ( (barFileUsed != null) && (p.matcher(barFileUsed).matches()) ){
                System.out.println(". Regex matched, adding flow for deletion...");
                flowsToDelete.add((Object)thisDeployedObject);
            } else {
                System.out.println(". Regex not matched, skipping...");
			}
		}
		
		if ( flowsToDelete.size() > 0) {
            println "Deleting "+flowsToDelete.size()+" deployed objects that are orphaned";
			println "Using timeout ${timeout}";
			
			// convert to DeployedObject [] to match deleteDeployedObjects method spec
			DeployedObject [] flowsToDeleteArray = new DeployedObject[flowsToDelete.size()];
            Iterator<DeployedObject> flowsIterator = flowsToDelete.iterator();

			int count = 0;
            while (flowsIterator.hasNext()) {
                flowsToDeleteArray[count++] = flowsIterator.next(); 
			}			

			executionGroupProxy.deleteDeployedObjects (flowsToDeleteArray , timeout);
			checkDeployResult();
		} 
        else {
			System.out.println("No orphaned flows to delete");
		}

	}
	
    public void deployMsgFlowConfig(String msgFlowName) {
        if (brokerProxy == null || bcp == null) {
            throw new IllegalStateException("Broker Proxy is uninitilized!");
        }
        
        if (executionGroupProxy == null) {
            throw new IllegalStateException("Execution group proxy is null! Make sure it is configured correctly!");
        }
        
        MessageFlowProxy msgFlowProxy = executionGroupProxy.getMessageFlowByName(msgFlowName);
        if ( msgFlowProxy == null ) {
            throw new Exception("could not get message flow to set property on!");
        }
        
        brokerProxy.deploy(60000);
        checkDeployResult();
    }
    
    public void checkDeployResult() {
        checkDeployResult(null);
    }
    public void checkDeployResult(def deployResult) {
        Enumeration logEntries = null;

        if (deployResult) {
            logEntries = deployResult.getDeployResponses();
        }
        else {
            logEntries = logProxy.elements();
        }

        def errors = [];

        while (logEntries.hasMoreElements()) {
            LogEntry logEntry = logEntries.nextElement();
            if (logEntry.isErrorMessage() && logEntry.getTimestamp() > startTime) {
                errors << logEntry.getTimestamp().toString() + " - " + logEntry.getMessage() + 
                        " : " + logEntry.getDetail();
            }
/*
            errors << logEntry.getTimestamp().toString() + " - " + logEntry.getMessage() + 
                    " : " + logEntry.getDetail();
*/
        }
        if (!errors.isEmpty()) {
            println "Errors during deployment";
            errors.each { error ->
                println error;
            }
            throw new Exception("Error during deployment");
        }
    }

    public void cleanUp() {
        if (isDebugEnabled) {
            BrokerProxy.disableAdministrationAPITracing();
        }
    }
	
	
}
