package com.urbancode.air.plugin.wmbcmp

import com.ibm.broker.config.proxy.BrokerProxy

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
            System.exit(0)
        }
        connection = connectionClass.newInstance(host, port, queueManager)
        proxy = BrokerProxy.getInstance(connection)
    }
}
