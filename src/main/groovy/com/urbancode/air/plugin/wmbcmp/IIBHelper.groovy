/**
 * (c) Copyright IBM Corporation 2013, 2017.
 * (c) Copyright HCL Technologies Ltd. 2018. All Rights Reserved.
 * This is licensed under the following license.
 * The Eclipse Public 1.0 License (http://www.eclipse.org/legal/epl-v10.html)
 * U.S. Government Users Restricted Rights:  Use, duplication or disclosure restricted by GSA ADP Schedule Contract with IBM Corp.
 */
package com.urbancode.air.plugin.wmbcmp

import com.ibm.broker.config.proxy.ApplicationProxy
import com.ibm.broker.config.proxy.AttributeConstants
import com.ibm.broker.config.proxy.BrokerConnectionParameters
import com.ibm.broker.config.proxy.BrokerProxy
import com.ibm.broker.config.proxy.ConfigurableService
import com.ibm.broker.config.proxy.ConfigManagerProxyRequestFailureException
import com.ibm.broker.config.proxy.CompletionCodeType
import com.ibm.broker.config.proxy.DeployedObject
import com.ibm.broker.config.proxy.DeployResult
import com.ibm.broker.config.proxy.ExecutionGroupProxy
import com.ibm.broker.config.proxy.LocalBrokerConnectionParameters
import com.ibm.broker.config.proxy.LogEntry
import com.ibm.broker.config.proxy.LogProxy
import com.ibm.broker.config.proxy.MessageFlowProxy
import com.urbancode.air.ExitCodeException
import com.urbancode.air.plugin.wmbcmp.IIB9BrokerConnection
import com.urbancode.air.plugin.wmbcmp.IIB10BrokerConnection
import java.text.SimpleDateFormat
import java.util.regex.Pattern

class IIBHelper {
    LogProxy logProxy
    BrokerProxy brokerProxy
    ExecutionGroupProxy executionGroupProxy
    boolean isDebugEnabled // Used for cleanup

    /**
     * Overloaded constructor to allow mocking for unit tests
     * @param brokerProxy
     * @param executionGroupProxy
     * @param logProxy
     * @param isDebugEnabled
     */
    public IIBHelper (
        BrokerProxy brokerProxy,
        ExecutionGroupProxy executionGroupProxy,
        LogProxy logProxy,
        boolean isDebugEnabled)
    {
        this.brokerProxy = brokerProxy
        this.executionGroupProxy = executionGroupProxy
        this.logProxy = logProxy
        this.isDebugEnabled = isDebugEnabled
    }

    public IIBHelper (Properties props) {
        // Local fields
        BrokerConnectionParameters bcp
        def host = props['brokerHost']
        def integrationNodeName = props['integrationNodeName']
        String version = props['version']
        boolean isIncremental = !Boolean.valueOf(props['fullDeploy'])
        int timeout = Integer.valueOf(props['timeout']?.trim()?:-1) // time to wait for broker configuration changes
        int versionInt
        def port
        def brokerConnection

        if (props['port']) {
            port = Integer.valueOf(props['port'])
        }

        // strip excess decimal points from version
        def integerPoint = version.indexOf('.') // point between integer and digits after the decimal point
        version = integerPoint != -1 ? version.substring(0, integerPoint) : version
        versionInt = version.toInteger()

        if (host && port) {
            println("${getTimestamp()} Establishing connection with a remote broker...")
            // iib9 remote broker connection
            if (versionInt < 10) {
                    // user determined by queue manager
                    def channel = props['channel']
                    def queueManager = props['queueManager']
                    brokerConnection = new IIB9BrokerConnection(host, port, queueManager, channel)
                    bcp = brokerConnection.connection
            }
            // iib10 remote connection settings
            else {
                // iib10 allows explicit identification of user for remote connection
                def user = props['username']
                def password = props['password']
                def useSSL = Boolean.valueOf(props['useSSL'])
                brokerConnection = new IIB10BrokerConnection(host, port, user, password, useSSL)

                bcp = brokerConnection.connection
            }
        }
        // local broker connection, regardless of iib version
        else if (integrationNodeName) {
            println("${getTimestamp()} Establishing connection with a local broker...")

            bcp = new LocalBrokerConnectionParameters(integrationNodeName)
            brokerProxy = BrokerProxy.getInstance(bcp)
        }
        else {
            throw new IllegalStateException("Must specify either an 'Integration Node Name' or an 'IP' and 'Port'.")
        }

        println("${getTimestamp()} Broker connection successful.")

        if (props['debugFile']) {
            isDebugEnabled = true
            BrokerProxy.enableAdministrationAPITracing(props['debugFile'])
        }

        // if the connection is not local
        if (!brokerProxy) {
            brokerProxy = brokerConnection.proxy
        }

        brokerProxy.setSynchronous(timeout)

        logProxy = brokerProxy.getLog()
    }

    public void stopAllMsgFlows() {
        if (brokerProxy == null) {
            throw new IllegalStateException("Broker Proxy is uninitialized!")
        }

        if (executionGroupProxy == null) {
            throw new IllegalStateException("Execution group proxy is null! Make sure it is configured correctly!")
        }

        executionGroupProxy.stopMessageFlows()
    }

    public void createExecutionGroupIfNeccessary(String executionGroup) {
        if (brokerProxy == null) {
            throw new IllegalStateException("Broker Proxy is uninitilized!")
        }

        if (!getExecutionGroup(executionGroup)) {
            System.out.println("${getTimestamp()} Execution group ${executionGroup} does not exist."
                    + " Attempting to create...")

            try {
                executionGroupProxy = brokerProxy.createExecutionGroup(executionGroup)
            }
            catch (ConfigManagerProxyRequestFailureException ex) {
                printBrokerResponses(ex)
                throw ex
            }

            if (executionGroupProxy == null) {
                throw new RuntimeException("Could not create execution group with"
                + " name ${name}")
            }
            System.out.println("${getTimestamp()} Execution group ${executionGroup} created.")
        }
        else {
            System.out.println("${getTimestamp()} Execution group ${executionGroup} exists."
                    + " Skipping create...")
        }
    }

    public void restartExecutionGroup(String executionGroup, int timeout) {
        setExecutionGroup(executionGroup)

        if (executionGroupProxy == null) {
            throw new RuntimeException("${getTimestamp()} Execution group ${executionGroup} does not exist.")
        }

        System.out.println("${getTimestamp()} Restarting execution group '${executionGroup}'.")
        try {
            if (executionGroupProxy.isRunning()) {
                System.out.println("${getTimestamp()} Stopping execution group '${executionGroup}'...")
                executionGroupProxy.stop()
                int count = 0

                // wait for server to fully stop
                while (executionGroupProxy.isRunning() && (timeout == -1 || count < timeout)) {
                    System.out.println("${getTimestamp()} Checking that '${executionGroup}' has stopped...")
                    Thread.sleep(3000)
                    count += 3000
                }

                if (timeout != -1 && count >= timeout) {
                    throw new IllegalStateException("The execution group '${executionGroup}' was unable to stop within "
                        + "the given timeout of '${timeout}' milliseconds.")
                }

                System.out.println("${getTimestamp()} Starting the execution group '${executionGroup}'...")
                executionGroupProxy.start()

                count = 0

                // wait for server to start
                while (!executionGroupProxy.isRunning() && (timeout == -1 || count < timeout)) {
                    System.out.println("${getTimestamp()} Checking that '${executionGroup}' has started...")
                    Thread.sleep(3000)
                    count += 3000
                }

                if (timeout != -1 && count >= timeout) {
                    throw new IllegalStateException("The execution group '${executionGroup}' was unable to start within "
                        + "the given timeout of '${timeout}' milliseconds.")
                }
            }
            else {
                throw new IllegalStateException("The execution group '${executionGroup}' is not currently running.")
            }
        } catch (ConfigManagerProxyRequestFailureException ex) {
            printBrokerResponses(ex)
            throw ex
        }
        System.out.println("${getTimestamp()} Successfully restarted '${executionGroup}'.")
    }

    public deleteExecutionGroup(String executionGroup, long timeout) {
        setExecutionGroup(executionGroup)

        println("${getTimestamp()} Deleting execution group '${executionGroup}'.")
        brokerProxy.deleteExecutionGroup(executionGroup, timeout)
        println("${getTimestamp()} Successfully deleted execution group.")
    }

    public void deleteConfigurableService(String servType, String servName) {
        if (brokerProxy == null) {
            throw new IllegalStateException("Broker Proxy is uninitilized!")
        }
        System.out.println("${getTimestamp()} Deleting Configurable Service '${servName}' of type '${servType}'")
        brokerProxy.deleteConfigurableService(servType, servName)
        System.out.println("${getTimestamp()} Successfully deleted Configurable Service.")
    }

    public void createOrUpdateConfigurableService(
        String servType,
        String servName,
        Map<String,String> propsMap,
        boolean deleteMissing)
    {
        if (brokerProxy == null) {
            throw new IllegalStateException("Broker Proxy is uninitilized!")
        }

        ConfigurableService service = brokerProxy.getConfigurableService(servType, servName)
        if (service == null) {
            println "${getTimestamp()} Creating configurable service '${servName}'..."
            createConfigurableService(servType, servName, propsMap)
        }
        else {
            updateConfigurableService(service, propsMap, servName, servType, deleteMissing)
        }
    }

    private void createConfigurableService(String servType, String servName, Map<String,String>propsMap) {
        println "${getTimestamp()} Creating configurable service '${servName}' of type '${servType}'"
        try {
            brokerProxy.createConfigurableService(servType, servName)
        } catch (ConfigManagerProxyRequestFailureException ex) {
            printBrokerResponses(ex)
            throw ex
        }
        // Note: https://developer.ibm.com/answers/questions/327647/examples-of-ibm-integration-api-creating-configura.html
        ConfigurableService service = brokerProxy.getConfigurableService(servType, servName)
        propsMap.each { key, value ->
            println "${getTimestamp()} Setting property '${key}' = '${value}'"
            service.setProperty(key, value)
        }

        println("${getTimestamp()} Successfully created configurable service.")
    }

    private void updateConfigurableService(
        ConfigurableService service,
        Map<String,String>propsMap,
        String servName,
        String servType,
        boolean deleteMissing)
    {
        println "${getTimestamp()} Updating configurable service '${servName}' of type '${servType}'"

        if (deleteMissing) {
            def keysToDelete = []
            service.getProperties().each { key,value ->
                if (!propsMap.containsKey(key)) {
                    println "Deleting property no longer in property map : '${key}' : '${value}'"
                    keysToDelete << key
                }
            }

            service.deleteProperties(keysToDelete as String[])
            println ("${getTimestamp()} Successfully removed all properties no longer in the property map.")
        }

        propsMap.each { key, value ->
            println "Setting property '${key}' = '${value}'"
            service.setProperty(key, value)
        }

        println ("${getTimestamp()} Successfully updated configurable service.")
    }

    public void startAllMsgFlows() {
        if (brokerProxy == null) {
            throw new IllegalStateException("Broker Proxy is uninitilized!")
        }

        if (executionGroupProxy == null) {
            throw new IllegalStateException("Execution group proxy is null! Make sure it is configured correctly!")
        }

        executionGroupProxy.startMessageFlows()
    }

    /*
     * Deploy a BAR file to execution groups
     * @param fileName the name of the BAR file to deploy
     * @return the completion code returned by the broker
     */
    public CompletionCodeType deployBrokerArchive(
        String fileName,
        String executionGroup,
        boolean isIncremental,
        int timeout)
    {
        if (brokerProxy == null) {
            throw new IllegalStateException("Broker Proxy is uninitilized!")
        }

        if (executionGroupProxy == null) {
            throw new IllegalStateException("Execution group proxy is null! Make sure it is configured correctly!")
        }

        Date startTime = new Date(System.currentTimeMillis())

        println "${getTimestamp()} Using execution group: ${executionGroup} and waiting with a timeout " +
            "of ${timeout} or until a response is received from the execution group..."
        DeployResult dr = executionGroupProxy.deploy(fileName, isIncremental, timeout)
        CompletionCodeType completionCode = dr.getCompletionCode()

        println("${getTimestamp()} Received deployment result from broker with a completion code: ${completionCode.toString()}.")

        checkDeployResult(dr, startTime)
        return completionCode
    }

    public void startMsgFlow(String msgFlowName) {
        if (brokerProxy == null) {
            throw new IllegalStateException("Broker Proxy is uninitilized!")
        }

        if (executionGroupProxy == null) {
            throw new IllegalStateException("Execution group proxy is null! Make sure it is configured correctly!")
        }

        MessageFlowProxy msgFlowProxy = executionGroupProxy.getMessageFlowByName(msgFlowName)
        if ( msgFlowProxy == null ) {
            throw new Exception("could not get message flow to start!")
        }
        msgFlowProxy.start()
    }

    public void stopMsgFlow(String msgFlowName) {
        if (brokerProxy == null) {
            throw new IllegalStateException("Broker Proxy is uninitilized!")
        }

        if (executionGroupProxy == null) {
            throw new IllegalStateException("Execution group proxy is null! Make sure it is configured correctly!")
        }

        MessageFlowProxy msgFlowProxy = executionGroupProxy.getMessageFlowByName(msgFlowName)
        if ( msgFlowProxy == null ) {
            throw new Exception("Could not get message flow to start!")
        }
        msgFlowProxy.stop()

    }

    public void setBrokerProperty(String name, String value, String propType) {
        if (brokerProxy == null) {
            throw new IllegalStateException("Broker Proxy is uninitilized!")
        }

        final String delimiter = AttributeConstants.OBJECT_NAME_DELIMITER

        try {
            if (propType.equalsIgnoreCase("cachemanager")) {
                String oldVal = brokerProxy.getCacheManagerProperty(name)

                println("Setting CacheManager property '${name}' from '${oldVal}' to '${value}'")
                brokerProxy.setCacheManagerProperty(name, value)
            }
            else if (propType.equalsIgnoreCase("configurableservice")) {
                String oldVal = brokerProxy.getConfigurableServiceProperty(name)

                println("Setting ConfigurableService property '${name}' from '${oldVal}' to '${value}'")
                brokerProxy.setConfigurableServiceProperty(name, value)
            }
            else if (propType.equalsIgnoreCase("httplistener")) {
                String oldVal = brokerProxy.getHTTPListenerProperty(name)

                println("Setting HTTPListener property '${name}' from '${oldVal}' to '${value}'")
                brokerProxy.setHTTPListenerProperty(name, value)
            }
            else if (propType.equalsIgnoreCase("securitycache")) {
                String oldVal = brokerProxy.getSecurityCacheProperty(name)

                println("Setting SecurityCache property '${name}' from '${oldVal}' to '${value}'")
                brokerProxy.setSecurityCacheProperty(name, value)
            }
            else if (propType.equalsIgnoreCase("webadmin")) {
                String fullName = "BrokerRegistry${delimiter}WebAdmin${delimiter}${name}"
                String oldVal = brokerProxy.getRegistryProperty(fullName)
                println("Setting WebAdmin property '${fullName}' from '${oldVal}' to '${value}'")
                brokerProxy.setRegistryProperty(fullName, value)
                println("Successfully set property value")
            }
            else {
                throw new IllegalStateException("${propType} is an unsupported component type.")
            }
        }
        catch (ConfigManagerProxyRequestFailureException ex) {
            printBrokerResponses(ex)
            throw ex
        }
    }

    public void setExecutionGroupProperty(String name, String value) {
        if (brokerProxy == null) {
            throw new IllegalStateException("Broker Proxy is uninitilized!")
        }

        if (executionGroupProxy == null) {
            throw new IllegalStateException("Execution group proxy is null! Make sure it is configured correctly!")
        }

        String oldVal = executionGroupProxy.getRuntimeProperty(name)
        String executionGroup = executionGroupProxy.getRuntimeProperty("This/label")
        println("${getTimestamp()} Setting property ${name} to ${value} from ${oldVal} on Execution Group "
            + "${executionGroup}!")
        try {
            executionGroupProxy.setRuntimeProperty(name, value)
        }
        catch (ConfigManagerProxyRequestFailureException ex) {
            printBrokerResponses(ex)
            throw ex
        }
        println("${getTimestamp()} Successfully set execution group property.")
    }

    public void setMsgFlowProperty(String msgFlowName, String name, String value, String executionGroup) {
        if (brokerProxy == null) {
            throw new IllegalStateException("Broker Proxy is uninitilized!")
        }

        if (executionGroupProxy == null) {
            throw new IllegalStateException("Execution group proxy is null! Make sure it is configured correctly!")
        }

        MessageFlowProxy msgFlowProxy = executionGroupProxy.getMessageFlowByName(msgFlowName)
        if ( msgFlowProxy == null ) {
            throw new Exception("could not get message flow to set property on!")
        }

        String oldVal = msgFlowProxy.getRuntimeProperty(name)
        println("${getTimestamp()} Setting property ${name} to ${value} from ${oldVal} on Message Flow "
            + "${msgFlowName} in Execution Group ${executionGroup}!")
        msgFlowProxy.setRuntimeProperty(name, value)
        println("${getTimestamp()} Successfully set message flow property.")
    }

    public void deleteMessageFlowsMatchingRegex(String regex, boolean deleteLibs, int timeout) {
        if (brokerProxy == null) {
            throw new IllegalStateException("Broker Proxy is uninitilized!")
        }

        if (executionGroupProxy == null) {
            throw new IllegalStateException("Execution group proxy is null! Make sure it is configured correctly!")
        }

        // Compile the regex
        Pattern p = Pattern.compile(regex)

        // List the flow names from the current BAR file
        Set<DeployedObject> flowsToDelete = new HashSet<DeployedObject>()

        // Get an unfiltered enumeration of all message flows in this execution group
        Enumeration<DeployedObject> allDeployedObjectsInThisEG

        allDeployedObjectsInThisEG = executionGroupProxy.getDeployedObjects()

        while (allDeployedObjectsInThisEG.hasMoreElements()) {
            DeployedObject thisDeployedObject = allDeployedObjectsInThisEG.nextElement()
            String barFileUsed = thisDeployedObject.getBARFileName()
            System.out.println("[OK] ${thisDeployedObject.getFullName()} was deployed with BAR file ${barFileUsed}.")
            if ( (barFileUsed != null) && (p.matcher(barFileUsed).matches()) ){
                if (!deleteLibs
                    && executionGroupProxy.getLibraryByName(thisDeployedObject.getFullName()) != null)
                {
                    println("[Action] Omitting library ${thisDeployedObject.getFullName()} from deletion.")
                }
                else {
                    System.out.println("[Action] Adding ${thisDeployedObject.getFullName()} for deletion...")
                    flowsToDelete.add((Object)thisDeployedObject)
                }
            } else {
                System.out.println("[OK] Regex not matched, skipping...")
            }
        }

        if ( flowsToDelete.size() > 0) {
            println "[Action] ${getTimestamp()} Deleting "+flowsToDelete.size()+" deployed objects that are orphaned"
            println "[OK] Waiting with a timeout of ${timeout} or until a response is received from the execution group..."

            Date startTime = new Date(System.currentTimeMillis())

            // convert to DeployedObject [] to match deleteDeployedObjects method spec
            DeployedObject [] flowsToDeleteArray = new DeployedObject[flowsToDelete.size()]
            Iterator<DeployedObject> flowsIterator = flowsToDelete.iterator()

            int count = 0
            while (flowsIterator.hasNext()) {
                flowsToDeleteArray[count++] = flowsIterator.next()
            }

            executionGroupProxy.deleteDeployedObjects (flowsToDeleteArray, timeout)

            println("${getTimestamp()} Deployment requested, waiting for result...")

            checkDeployResult(startTime)
        }
        else {
            System.out.println("No orphaned flows to delete")
        }

    }

    public void deleteApplications(List<String> appNames, String executionGroup, int timeout, boolean failFast) {
        def fullAppNames = []

        for (appName in appNames) {
            println("[Action] ${getTimestamp()} Searching for application with name ${appName}...")
            ApplicationProxy appProxy = executionGroupProxy.getApplicationByName(appName)

            if (appProxy != null) {
                fullAppNames << appProxy.getFullName()
            }
            else {
                if (failFast) {
                    throw new RuntimeException("Application: ${appName} isn't currently deployed to the execution "
                        + "group: ${executionGroup}.")
                }
                else {
                    println("[Warning] Application: ${appName} isn't currently deployed to the execution group: "
                        + "${executionGroup}.")
                }
            }
        }

        if (fullAppNames) {
            String[] appArray = fullAppNames.toArray(new String[0])
            println("[Action] ${getTimestamp()} Deleting application(s): ${fullAppNames.join(',')}")
            Date startTime = new Date(System.currentTimeMillis())
            DeployResult dr = executionGroupProxy.deleteDeployedObjectsByName(appArray, timeout)
            checkDeployResult(dr, startTime)
            println("[OK] ${getTimestamp()} Successfully removed applications.")
        }
        else {
            throw new RuntimeException("None of the specified applications exist on the execution group: ${executionGroup}.")
        }
    }

    public void checkDeployResult(def startTime) {
        checkDeployResult(null, startTime)
    }

    public void checkDeployResult(def deployResult, def startTime) {
        Enumeration logEntries = null

        if (deployResult) {
            println("${getTimestamp()} Acquiring all deployment messages associated with this deployment...")
            logEntries = deployResult.getDeployResponses()
        }
        else {
            logEntries = logProxy.elements()
        }

        def errors = []

        println("${getTimestamp()} Checking deployment messages for errors...")
        while (logEntries.hasMoreElements()) {
            LogEntry logEntry = logEntries.nextElement()
            if (logEntry.isErrorMessage() && logEntry.getTimestamp() > startTime) {
                errors << logEntry.getTimestamp().toString() + " - " + logEntry.getMessage() +
                        " : " + logEntry.getDetail()
            }
        }
        if (!errors.isEmpty()) {
            println "Errors during deployment"
            errors.each { error ->
                println error
            }
            throw new Exception("Error during deployment")
        }

        println("${getTimestamp()} No errors found during the deployment.")
    }

    public void cleanUp() {
        println("${getTimestamp()} Disconnecting from the broker...")
        if (isDebugEnabled) {
            BrokerProxy.disableAdministrationAPITracing()
        }

        brokerProxy.disconnect()
        println("${getTimestamp()} Successfully disconnected from broker.")
    }

    private ExecutionGroupProxy getExecutionGroup(String groupName) {
        return brokerProxy.getExecutionGroupByName(groupName)
    }

    public void setExecutionGroup(String groupName) {
        if (groupName) {
            executionGroupProxy = getExecutionGroup(groupName)

            if (executionGroupProxy == null) {
                throw new IllegalStateException("Execution group ${groupName} does not exist. Please make sure its " +
			             "name is spelled correctly and that it exists under the specified broker.")
            }
        }
        else {
            throw new IllegalStateException("Execution group field specified with blank or null name.")
        }
    }

    public static String getTimestamp() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("[MM/dd/yyyy HH:mm:ss]")

        return dateFormat.format(new Date())
    }

    private void printBrokerResponses(ConfigManagerProxyRequestFailureException ex) {
        List<LogEntry> responses = ex.getResponseMessages()

        println("Broker rejection errors during execution:")
        for (LogEntry entry : responses) {
            if (entry.isErrorMessage() && entry.getTimestamp() > startTime) {
                println("[${entry.getTimestamp().toString()}] ${entry.getMessage()}"
                    + " : ${entry.getDetail()}")
            }
        }
        println ("")
        println ("[Service Information] " + ex.getServiceInformation())
    }
}
