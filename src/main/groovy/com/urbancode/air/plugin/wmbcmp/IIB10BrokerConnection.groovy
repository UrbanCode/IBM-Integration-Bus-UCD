package com.urbancode.air.plugin.wmbcmp

import com.ibm.broker.config.proxy.BrokerProxy
import com.ibm.broker.config.proxy.ConfigManagerProxyLoggedException

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

        if (user && password || useSSL) {
            connection = connectionClass.newInstance(host, port, user, password, useSSL)
        }
        else {
            connection = connectionClass.newInstance(host, port)
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