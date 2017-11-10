/**
 * (c) Copyright IBM Corporation 2013, 2017.
 * This is licensed under the following license.
 * The Eclipse Public 1.0 License (http://www.eclipse.org/legal/epl-v10.html)
 * U.S. Government Users Restricted Rights:  Use, duplication or disclosure restricted by GSA ADP Schedule Contract with IBM Corp.
 */
import com.urbancode.air.AirPluginTool;
import com.urbancode.air.plugin.wmbcmp.IIBHelper;

File workDir = new File('.').canonicalFile

AirPluginTool apTool = new AirPluginTool(this.args[0], this.args[1])
def props = apTool.getStepProperties(System.getenv("UCD_SECRET_VAR"))
def helper = new IIBHelper(props)

def executionGroup = props['executionGroup']
def dirOffset = props['dirOffset']
def propertyFile = props['propertyFile']

if (dirOffset) {
    workDir = new File(workDir, dirOffset).canonicalFile
}
try {
    helper.setExecutionGroup(executionGroup)

    if (propertyFile) {
        def file = new File(workDir, propertyFile).canonicalFile
        if (file.exists()) {
            println "Found property file $file, importing properties..."
            if (props['properties']) {
                println "WARNING! Ignoring values from the Properties field!"
            }
            def fileProps = new Properties()
            def flowSet = [] as Set
            file.withInputStream { stream ->
                fileProps.load(stream)
            }
            fileProps.each {
                def index = it.key.indexOf('#')
                if (index > 0 && index < it.key.length()) {
                    def flowName = it.key.substring(0, index)
                    def propertyName = it.key.substring(index + 1)
                    println "Setting $propertyName to ${it.value} for message flow ${it}"
                    helper.setMsgFlowProperty(flowName, propertyName, it.value)
                    flowSet.add(flowName)
                }
                else {
                    println "Found invalid property ${it.key}=${it.value}"
                }
            }
        }
        else {
            throw new Exception("Property file $file does not exist!")
        }
    }
    else {
        if (!props['msgFlows'] || !props['properties']) {
            throw new Exception('Properties and Message Flows are required fields if not using a property file!')
        }

        props['msgFlows'].split('\n').each { msgFlow ->
            println "Setting properties on message flow ${msgFlow}"
            props['properties'].split('\n').each { property ->
                String name = property.split('=',2)[0]
                String value = property.split('=',2)[1]
                helper.setMsgFlowProperty(msgFlow, name, value, executionGroup)
            }
        }
    }
}
catch (Exception e) {
    e.printStackTrace();
    throw e;
}
finally {
    helper.cleanUp();
}
