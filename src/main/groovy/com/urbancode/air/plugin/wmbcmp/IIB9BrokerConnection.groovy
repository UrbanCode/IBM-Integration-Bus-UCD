/**
 * © Copyright IBM Corporation 2015, 2017.
 * This is licensed under the following license.
 * The Eclipse Public 1.0 License (http://www.eclipse.org/legal/epl-v10.html)
 * U.S. Government Users Restricted Rights:  Use, duplication or disclosure restricted by GSA ADP Schedule Contract with IBM Corp.
 */

import com.ibm.broker.config.proxy.BrokerProxy
import com.ibm.broker.config.proxy.ConfigManagerProxyLoggedException

class IIB9BrokerConnection {
    def connection
    BrokerProxy proxy

    public IIB9BrokerConnection(def host, def port, def queueManager, def channel) {
        def connectionClass
        try {
            connectionClass = "com.ibm.broker.config.proxy.MQBrokerConnectionParameters" as Class
        }
        catch(Exception ex) {
            println("com.ibm.broker.config.proxy.MQBrokerConnectionParameters class not found")
            println(ex.printStackTrace())
            System.exit(1)
        }

        connection = connectionClass.newInstance(host, port, queueManager)

        if (channel) {
            connection.setAdvancedConnectionParameters(channel, null,null, -1, -1, null)
        }

        try {
            proxy = BrokerProxy.getInstance(connection)
        }
        catch(ConfigManagerProxyLoggedException ex) {
            println("Could not establish a connection with the broker host: ${host} using port: ${port}")
            println(ex.getMessage())
            System.exit(1)
        }
    }
}
