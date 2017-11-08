package src.test.groovy

import groovy.util.GroovyTestCase

import com.urbancode.sync.*
import com.urbancode.plugin.*

import spock.lang.Specification
import spock.lang.*

import com.urbancode.plugin.test.mocks.*
import com.urbancode.plugin.test.data.*
import com.ibm.broker.config.proxy.BrokerProxy
import com.ibm.broker.config.proxy.ExecutionGroupProxy
import com.ibm.broker.config.proxy.LogProxy
import com.ibm.broker.config.proxy.LogEntry
import com.ibm.broker.config.proxy.DeployedObject
import com.ibm.broker.config.proxy.LibraryProxy

import com.urbancode.air.plugin.wmbcmp.IIBHelper

@Unroll
class DeleteMessFlowsTest extends Specification {

    def "IIBHelper deleteMessFlowsMatchingRegex can exclude libraries"() {
        setup:
            def brokerProxyMock = Mock(BrokerProxy)
            def exGroupProxyMock = Mock(ExecutionGroupProxy)
            def logProxyMock = Mock(LogProxy)
            def messFlowMock = Mock(DeployedObject)
            def sharedLibraryMock = Mock(DeployedObject)
            def libraryProxyMock = Mock(LibraryProxy)
            def iibHelper = new IIBHelper(brokerProxyMock, exGroupProxyMock, logProxyMock, true)

            Vector<DeployedObject> objectVector = new Vector<DeployedObject>()
            objectVector.add(messFlowMock)
            objectVector.add(sharedLibraryMock)
            Enumeration<DeployedObject> deployedObjects = objectVector.elements()

            Vector<LogEntry> logVector = new Vector<LogEntry>()
            Enumeration<LogEntry> logs = logVector.elements()
            logProxyMock.elements() >> logs

            messFlowMock.getBARFileName() >> "MyBarFile"
            sharedLibraryMock.getBARFileName() >> "MyBarFile"
            messFlowMock.getFullName() >> "MyMessFlow"
            sharedLibraryMock.getFullName() >> "MyLib"
            exGroupProxyMock.getLibraryByName(messFlowMock.getFullName()) >> null
            exGroupProxyMock.getLibraryByName(sharedLibraryMock.getFullName()) >> libraryProxyMock
            exGroupProxyMock.getDeployedObjects() >> deployedObjects
        when:
            iibHelper.deleteMessageFlowsMatchingRegex("MyBarFile", false, -1)
        then:
             1 * exGroupProxyMock.deleteDeployedObjects(*_) >> {arguments ->
                 assert arguments[0].length == 1
                 assert arguments[0][0] == messFlowMock
             }
    }
}