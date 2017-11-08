/**
 * (c) Copyright IBM Corporation 2013, 2017.
 * This is licensed under the following license.
 * The Eclipse Public 1.0 License (http://www.eclipse.org/legal/epl-v10.html)
 * U.S. Government Users Restricted Rights:  Use, duplication or disclosure restricted by GSA ADP Schedule Contract with IBM Corp.
 */
package com.urbancode.air.plugin.wmbcmp

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
import com.ibm.broker.config.proxy.MessageFlowProxy
import com.urbancode.air.ExitCodeException
import com.urbancode.air.plugin.wmbcmp.IIB9BrokerConnection
import com.urbancode.air.plugin.wmbcmp.IIB10BrokerConnection
import java.text.SimpleDateFormat
import java.util.regex.Pattern

class IIBHelper {
    def logProxy
    def executionGroups
    def version
	def versionInt
    def brokerConnection
    Integer timeout
    Date startTime
    boolean isDebugEnabled
    boolean isIncremental
    BrokerConnectionParameters bcp
    BrokerProxy brokerProxy
    ExecutionGroupProxy executionGroupProxy

    public IIBHelper(Properties props) {
        def host = props['brokerHost']
        def port
        def integrationNodeName = props['integrationNodeName']

        if (props['port']) {
            port = Integer.valueOf(props['port'])
        }

        // strip excess decimal points
        version = props['version']
        def integerPoint = version.indexOf('.') // point between integer and digits after the decimal point
        version = integerPoint != -1 ? version.substring(0, integerPoint) : version
        versionInt = version.toInteger()

        timeout = Integer.valueOf(props['timeout']?.trim()?:-1) // time to wait for broker response
        isIncremental = !Boolean.valueOf(props['fullDeploy'])

        if (host && port) {
            //iib9 remote broker connection
            if (versionInt < 10) {
                    //user determined by queue manager
                    def channel = props['channel']
                    def queueManager = props['queueManager']
                    brokerConnection = new IIB9BrokerConnection(host, port, queueManager, channel)
                    bcp = brokerConnection.connection
            }
            //iib10 remote connection settings
            else {
                //iib10 allows explicit identification of user for remote connection
                def user = props['username']
                def password = props['password']
                def useSSL = Boolean.valueOf(props['useSSL'])
                brokerConnection = new IIB10BrokerConnection(host, port, user, password, useSSL)

                bcp = brokerConnection.connection
            }
        }
        //local broker connection, regardless of iib version
        else if (integrationNodeName) {
            println("${getTimestamp()} Establishing connection with a local broker...")

            bcp = new LocalBrokerConnectionParameters(integrationNodeName)
            brokerProxy = BrokerProxy.getInstance(bcp)

            def properties = BrokerProxy.getProperties()
            properties.toString()
        }
        else {
            throw new IllegalStateException("Must specify either an 'Integration Node Name' or an 'IP' and 'Port'.")
        }

        if (props['debugFile']) {
            isDebugEnabled = true
            BrokerProxy.enableAdministrationAPITracing(props['debugFile'])
        }

        // if the connection is not local
        if (!brokerProxy) {
            brokerProxy = brokerConnection.proxy
        }

        brokerProxy.setSynchronous(timeout)

        startTime = new Date(System.currentTimeMillis())
        logProxy = brokerProxy.getLog()
    }

    public void stopAllMsgFlows() {
        if (brokerProxy == null || bcp == null) {
            throw new IllegalStateException("Broker Proxy is uninitialized!")
        }

        if (executionGroupProxy == null) {
            throw new IllegalStateException("Execution group proxy is null! Make sure it is configured correctly!")
        }

        executionGroupProxy.stopMessageFlows()
    }

    public void createExecutionGroupIfNeccessary(String executionGroup) {
        if (brokerProxy == null || bcp == null) {
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

    public void restartExecutionGroup(String executionGroup) {
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

    public void deleteConfigurableService(String servType, String servName) {
        if (brokerProxy == null || bcp == null) {
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
        if (brokerProxy == null || bcp == null) {
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
        if (brokerProxy == null || bcp == null) {
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
    public CompletionCodeType deployBrokerArchive(String fileName, String executionGroup) {
        if (brokerProxy == null || bcp == null) {
            throw new IllegalStateException("Broker Proxy is uninitilized!")
        }

        if (executionGroupProxy == null) {
            throw new IllegalStateException("Execution group proxy is null! Make sure it is configured correctly!")
        }

        println "${getTimestamp()} Using execution group: ${executionGroup} and waiting with a timeout " +
            "of ${timeout} or until a response is received from the execution group..."
        DeployResult dr = executionGroupProxy.deploy(fileName, isIncremental, timeout)
        CompletionCodeType completionCode = dr.getCompletionCode()

        checkDeployResult(dr)
        return completionCode
    }

    public void startMsgFlow(String msgFlowName) {
        if (brokerProxy == null || bcp == null) {
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
        if (brokerProxy == null || bcp == null) {
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
        if (brokerProxy == null || bcp == null) {
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
        if (brokerProxy == null || bcp == null) {
            throw new IllegalStateException("Broker Proxy is uninitilized!")
        }

        if (executionGroupProxy == null) {
            throw new IllegalStateException("Execution group proxy is null! Make sure it is configured correctly!")
        }

        String oldVal = executionGroupProxy.getRuntimeProperty(name)
        String executionGroup = executionGroupProxy.getRuntimeProperty("This/label")
        println("${getTimestamp()} Setting property ${name} to ${value} from ${oldVal} on Execution Group "
            + "${executionGroup}!")
        executionGroupProxy.setRuntimeProperty(name, value)
        println("${getTimestamp()} Successfully set execution group property.")
    }

    public void setMsgFlowProperty(String msgFlowName, String name, String value, String executionGroup) {
        if (brokerProxy == null || bcp == null) {
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

    public void deleteMessageFlowsMatchingRegex(String regex, boolean deleteSharedLibs) {
        if (brokerProxy == null || bcp == null) {
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
                if (!deleteSharedLibs
                    && executionGroupProxy.getSharedLibraryByName(thisDeployedObject.getFullName()) != null)
                {
                    println("[Action] Omitting shared library ${thisDeployedObject.getFullName()} from deletion.")
                    continue
                }

                System.out.println("[Action] Adding ${thisDeployedObject.getFullName()} for deletion...")
                flowsToDelete.add((Object)thisDeployedObject)
            } else {
                System.out.println("[OK] Regex not matched, skipping...")
            }
        }

        if ( flowsToDelete.size() > 0) {
            println "[Action] ${getTimestamp()} Deleting "+flowsToDelete.size()+" deployed objects that are orphaned"
            println "[OK] Waiting with a timeout of ${timeout} or until a response is received from the execution group..."

            // convert to DeployedObject [] to match deleteDeployedObjects method spec
            DeployedObject [] flowsToDeleteArray = new DeployedObject[flowsToDelete.size()]
            Iterator<DeployedObject> flowsIterator = flowsToDelete.iterator()

            int count = 0
            while (flowsIterator.hasNext()) {
                flowsToDeleteArray[count++] = flowsIterator.next()
            }

            executionGroupProxy.deleteDeployedObjects (flowsToDeleteArray, timeout)
            checkDeployResult()
        }
        else {
            System.out.println("No orphaned flows to delete")
        }

    }

    public void checkDeployResult() {
        checkDeployResult(null)
    }

    public void checkDeployResult(def deployResult) {
        Enumeration logEntries = null

        if (deployResult) {
            logEntries = deployResult.getDeployResponses()
        }
        else {
            logEntries = logProxy.elements()
        }

        def errors = []

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
    }

    public void cleanUp() {
        if (isDebugEnabled) {
            BrokerProxy.disableAdministrationAPITracing()
        }

        brokerProxy.disconnect()
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

    public String getTimestamp() {
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
