package com.urbancode.air.plugin.wmbcmp

import com.ibm.broker.config.proxy.BrokerProxy

class IIB10BrokerConnection {
    def connection
    BrokerProxy proxy

    public IIB10BrokerConnection(def host, def port, def user, def password, def useSSL) {
        def connectionClass
        try {
            connectionClass = "com.ibm.broker.config.proxy.IntegrationNodeConnectionParameters" as Class
        }
        catch(Exception ex) {
            println("com.ibm.broker.config.proxy.IntegrationNodeConnectionParameters class not found")
            println(ex.printStackTrace())
            System.exit(1)
        }

        if (user) {
            connection = connectionClass.newInstance(host, port, user, password, useSSL)
        }
        else {
            connection = connectionClass.newInstance(host, port)
        }

        proxy = BrokerProxy.getInstance(connection)
    }
}
