package com.urbancode.air.plugin.wmbcmp

import com.ibm.broker.config.proxy.BrokerProxy
import com.ibm.broker.config.proxy.ConfigManagerProxyLoggedException

class IIB9BrokerConnection {
    def connection
    BrokerProxy proxy

    public IIB9BrokerConnection(def host, def port, def queueManager) {
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