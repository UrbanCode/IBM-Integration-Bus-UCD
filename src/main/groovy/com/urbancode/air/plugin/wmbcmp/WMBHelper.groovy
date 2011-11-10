package com.urbancode.air.plugin.wmbcmp;

import com.ibm.broker.config.proxy.*;
import java.util.Properties;

public class WMBHelper {

    String host;
    int port;
    String queueManager;
    String executionGroup;
    BrokerConnectionParameters bcp;
    BrokerProxy brokerProxy;
    ExecutionGroupProxy executionGroupProxy;
    boolean isIncremental = true;

    public WMBHelper(Properties props) {
        host = props['brokerHost'];
        port = Integer.valueOf(props['port']);
        queueManager = props['queueManager'];
        executionGroup = props['executionGroup'];

        bcp = new MQBrokerConnectionParameters(host, port, queueManager);
        brokerProxy = BrokerProxy.getInstance(bcp);
        if (executionGroup != null && executionGroup.trim() != "") {
            executionGroupProxy = brokerProxy.getExecutionGroupByName(executionGroup);
        }

        if (Boolean.valueOf(props['fullDeploy'])) {
            isIncremental = false;
        }
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
        
        DeployResult dr = executionGroupProxy.deploy(fileName, isIncremental, 30000);
        
        if (dr.getCompletionCode() != CompletionCodeType.success) {
            throw new Exception("Failed deploying bar File ${fileName}!");
        }

        def logEntries = dr.getLogEntries();
        def errors = [];

        logEntries.each { logEntry ->
            if (logEntry.isErrorMessage()) {
                errors << logEntry.getMessage();
            }
        }
        if (!errors.isEmpty()) {
            println "Errors deploying barFile ${fileName}";
            errors.each { error ->
                println error;
            }
            throw new Exception("Failed deploying bar File ${fileName}!");
        }
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
    
    public void overrideBarProperties(String barFileName, Properties props) {
        //todo
        
    }
}
