package com.urbancode.air.plugin.wmbcmp;

import com.ibm.broker.config.proxy.*;
import java.util.Properties;
import java.util.Enumeration;

public class WMBHelper {

    String host;
    int port;
    String queueManager;
    String executionGroup;
    BrokerConnectionParameters bcp;
    BrokerProxy brokerProxy;
    ExecutionGroupProxy executionGroupProxy;
    boolean isIncremental = true;
    def configManProxy;
    def logProxy;
    Date startTime;
    def timeout;

    public WMBHelper(Properties props) {
        if (props['username']?.trim()) {
            System.out.println("Setting user.name to " + props['username']);
            System.setProperty("user.name", props['username']);
        }
        host = props['brokerHost'];
        port = Integer.valueOf(props['port']);
        queueManager = props['queueManager'];
        executionGroup = props['executionGroup'];
        timeout = Long.valueOf(props['timeout']?.trim()?:60000);

        bcp = new MQBrokerConnectionParameters(host, port, queueManager);
        brokerProxy = BrokerProxy.getInstance(bcp);
        if (executionGroup != null && executionGroup.trim() != "") {
            executionGroupProxy = brokerProxy.getExecutionGroupByName(executionGroup);
        }

        if (Boolean.valueOf(props['fullDeploy'])) {
            isIncremental = false;
        }
        
        startTime = new Date(System.currentTimeMillis());
        MQConfigManagerConnectionParameters connectionParams =  new MQConfigManagerConnectionParameters(host, port, queueManager);
        configManProxy = ConfigManagerProxy.getInstance(connectionParams);
        logProxy = configManProxy.getLog();
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
            throw new Exception("Failed deploying bar File ${fileName}!");
        }

        checkDeployResult();
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
        Enumeration logEntries = logProxy.elements();
        def errors = [];

        while (logEntries.hasMoreElements()) {
            LogEntry logEntry = logEntries.nextElement();
            if (logEntry.isErrorMessage() && logEntry.getTimestamp() > startTime) {
                errors << logEntry.getTimestamp().toString() + " - " + logEntry.getMessage() + 
                        " : " + logEntry.getDetail();
            }
        }
        if (!errors.isEmpty()) {
            println "Errors during deployment";
            errors.each { error ->
                println error;
            }
            throw new Exception("Error during deployment");
        }
    }
}
