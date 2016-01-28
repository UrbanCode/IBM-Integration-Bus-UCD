/*
 * Licensed Materials - Property of IBM Corp.
 * IBM UrbanCode Build
 * IBM UrbanCode Deploy
 * IBM UrbanCode Release
 * IBM AnthillPro
 * (c) Copyright IBM Corporation 2002, 2016. All Rights Reserved.
 *
 * U.S. Government Users Restricted Rights - Use, duplication or disclosure restricted by
 * GSA ADP Schedule Contract with IBM Corp.
 */
import com.urbancode.air.AirPluginTool;
import com.urbancode.air.CommandHelper;

final def apTool = new AirPluginTool(this.args[1], this.args[2])
final def PLUGIN_HOME = System.getenv("PLUGIN_HOME")
final def isWindows = apTool.isWindows
final def props = apTool.getStepProperties()

CommandHelper helper = new CommandHelper(new File('.').canonicalFile)
def argScript = PLUGIN_HOME + File.separator + this.args[0]
def classpath = PLUGIN_HOME + File.separator + "classes" + File.pathSeparator + props['jarPath']
def mqsiprofile = new File(props['mqsiprofile'].trim())
def groovyHome = System.getProperty("groovy.home")
def groovyExe = groovyHome + File.separator + "bin" + File.separator + "groovy"
def version = props['version'].trim()
def cmdArgs

if (props['mqsiprofile'].trim()) {
    if (!mqsiprofile.isFile()) {
        throw new FileNotFoundException("${mqsiprofile.absolutePath} is not a file on the file system.")
    }

    if (isWindows) {
        // set command environment
        def envMap = [:]

        def envSet = {
            envMap = System.getenv()
            it.out.close() // close stdin
            def out = new PrintStream(System.out, true)
            try {
                it.waitForProcessOutput(out, out) // forward stdout and stderr to autoflushing output stream
            }
            finally {
                out.flush();
            }
        }

        cmdArgs = [mqsiprofile.absolutePath]
        helper.runCommand("Setting up mqsi environment...", cmdArgs, envSet)

        for (def entry : envMap) {
            helper.addEnvironmentVariable(entry.key, entry.value)
        }

        // run script regardless of whether environment was set
        cmdArgs = [
            "${groovyExe}.bat",
            "-cp",
            classpath,
            argScript,
            args[1],
            args[2]
        ]
    }
    else {
        def defaultShell = props['shell']

        // source mqsiprofile within the same process as the groovy script to maintain command environment
        cmdArgs = [
            defaultShell,
            "-c",
            ". ${mqsiprofile.absolutePath} && ${groovyExe} -cp ${classpath} ${argScript} ${this.args[1]} ${this.args[2]}"
        ]
    }
}
else {
    cmdArgs = [
        groovyExe,
        "-cp",
        classpath,
        argScript,
        this.args[1],
        this.args[2]
    ]
}

helper.runCommand(cmdArgs.join(' '), cmdArgs)