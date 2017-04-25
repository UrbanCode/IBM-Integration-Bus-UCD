/**
 * (c) Copyright IBM Corporation 2013, 2017.
 * This is licensed under the following license.
 * The Eclipse Public 1.0 License (http://www.eclipse.org/legal/epl-v10.html)
 * U.S. Government Users Restricted Rights:  Use, duplication or disclosure restricted by GSA ADP Schedule Contract with IBM Corp.
 */
package com.urbancode.air.plugin.wmbcmp

import com.urbancode.air.CommandHelper
import com.urbancode.air.ExitCodeException

/**
 * Class to run MQSI script commands
 *
 */
class MQSIHelper {
    final def workDir = new File('.').canonicalFile
    CommandHelper chelper
    def installDir  // installation directory of iib server
    def cmdDir  // directory where mqsi script files exist
    def isWindows

    /**
     * Constructor for MQSIHelper class
     * @param installDir
     * @param isWindows
     */
    public MQSIHelper(def installDir, def isWindows) {
        chelper = new CommandHelper(workDir)
        this.installDir = installDir
        cmdDir = installDir + File.separator + "bin"
        this.isWindows = isWindows

        File commandDir = new File(cmdDir)

        if (!commandDir.isDirectory()) {
            throw new FileNotFoundException("The directory: ${cmdDir} does not exist.")
        }
    }

    /**
     * Create an integration node
     * @param integrationNode
     * @param defaultUser
     * @param defaultPass
     * @param queueManager
     * @param additionalArgs
     */
    void createBroker(def integrationNode, def queueManager, def serviceUser, def servicePass, def additionalArgs) {
        def mqsicreatebroker = createMQCommand("mqsicreatebroker")

        def args = []

        // a queue manager will be created if not specified
        if (queueManager) {
            args << "-q"
            args << queueManager
        }

        // iib 9 only required properties
        if (serviceUser) {
            args << "-i"
            args << serviceUser
        }

        if (servicePass) {
            args << "-a"
            args << servicePass
        }

        if (additionalArgs) {
            for (def arg : additionalArgs.split('\n')) {
                if (arg.trim()) {
                    args << arg.trim()
                }
            }
        }

        args << integrationNode

        runMQCommand("Creating integration node: ${integrationNode}...", mqsicreatebroker, args)
    }

    /**
     * Delete an integration node
     * @param integrationNode
     * @param deleteQueues
     * @param deleteTrace
     */
    void deleteBroker(def integrationNode, boolean deleteQueues, boolean deleteTrace) {
        def mqsideletebroker = createMQCommand("mqsideletebroker")

        def args = []

        if (deleteQueues) {
            args << "-s"
        }

        if (deleteTrace) {
            args << "-w"
        }

        args << integrationNode

        runMQCommand("Delete integration node: ${integrationNode}...", mqsideletebroker, args)
    }

    /**
     * Start an integration node
     * @param integrationNode
     */
    void startBroker(def integrationNode) {
        def mqsistart = createMQCommand("mqsistart")

        def args = [integrationNode]

        runMQCommand("Starting integration node: ${integrationNode}...", mqsistart, args)
    }

    /**
     * Stop an integration node
     * @param integrationNode
     */
    void stopBroker(def integrationNode) {
        def mqsistop = createMQCommand("mqsistop")

        def args = [integrationNode]

        runMQCommand("Stopping integration node: ${integrationNode}...", mqsistop, args)
    }

    /**
     * Acquire the specified mqsi script file in the command directory if it exists
     * @param scriptFile
     * @return String path to script file
     */
    private def createMQCommand(def scriptFile) {
        createMQCommand(scriptFile, "")
    }

    /**
     * Acquire the specified mqsi script file with the specified extension
     * @param scriptFile
     * @param extension
     * @return String path to script file
     */
    private def createMQCommand(def scriptFile, def extension) {
        if (isWindows) {
            if (!extension) {
                extension = ".exe"
            }

            scriptFile = scriptFile + extension
        }

        File mqFile = new File(cmdDir, scriptFile)

        if (!mqFile.isFile()) {
            throw new FileNotFoundException("Cannot find file ${mqFile.absolutePath}")
        }

        return mqFile.toString()
    }

    /**
     * Run a CommandHelper command
     * @param msg
     * @param command
     * @param args
     */
    private void runMQCommand(def msg, def command, def args) {
        def cmdArgs
        def mqsiprofile = createMQCommand("mqsiprofile", ".cmd")

        if (!isWindows) {
            def argString = ""

            for(def arg : args) {
                argString += "${arg} "
            }

            cmdArgs = ["/bin/bash", "-c", "source ${mqsiprofile}; ${command} ${argString}"]
        }
        else {
            cmdArgs = [mqsiprofile, "&", command]

            for (def arg : args) {
                cmdArgs << arg
            }
        }

        chelper.runCommand(msg, cmdArgs)
    }
}
