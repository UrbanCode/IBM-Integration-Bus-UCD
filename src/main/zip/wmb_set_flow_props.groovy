import com.urbancode.air.plugin.wmbcmp.*;
import com.urbancode.air.plugin.*;

File workDir = new File('.').canonicalFile

Properties props = PropertiesHelper.getProperties(new File(args[0]))
WMBHelper helper = new WMBHelper(props)

def dirOffset = props['dirOffset']
def propertyFile = props['propertyFile']

if (dirOffset) {
    workDir = new File(workDir, dirOffset).canonicalFile
}
try {
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
            flowSet.each {
                helper.deployMsgFlowConfig(it)
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
                helper.setMsgFlowProperty(msgFlow, name, value)
            }
        }

        props['msgFlows'].split('\n').each { msgFlow ->
            helper.deployMsgFlowConfig(msgFlow);
        }
    }
}
catch (Exception e) {
    e.printStackTrace();
    throw e;
}
