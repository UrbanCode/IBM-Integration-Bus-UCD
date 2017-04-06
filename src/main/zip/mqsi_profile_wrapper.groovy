/*
 * Licensed Materials - Property of IBM Corp.
 * IBM UrbanCode Build
 * IBM UrbanCode Deploy
 * IBM UrbanCode Release
 * IBM AnthillPro
 * (c) Copyright IBM Corporation 2002, 2017. All Rights Reserved.
 *
 * U.S. Government Users Restricted Rights - Use, duplication or disclosure restricted by
 * GSA ADP Schedule Contract with IBM Corp.
 */

import org.apache.commons.lang.StringUtils

import com.urbancode.air.AirPluginTool;
import com.urbancode.air.CommandHelper;

import java.util.regex.Pattern

final def apTool = new AirPluginTool(this.args[1], this.args[2])
final def PLUGIN_HOME = System.getenv("PLUGIN_HOME")
final def PLUGIN_LIB = PLUGIN_HOME + File.separator + "lib"
final def isWindows = apTool.isWindows
final def props = apTool.getStepProperties()

CommandHelper helper = new CommandHelper(new File('.').canonicalFile)
def argScript = PLUGIN_HOME + File.separator + this.args[0]
def jarPath = props['jarPath'].trim()
def classpath = new StringBuilder(PLUGIN_HOME + File.separator + "classes")
classpath.append(File.pathSeparator + PLUGIN_LIB + File.separator + "jettison-1.1.jar")
classpath.append(File.pathSeparator + PLUGIN_LIB + File.separator + "CommonsUtil.jar")
classpath.append(File.pathSeparator + PLUGIN_LIB + File.separator + "securedata.jar")
classpath.append(File.pathSeparator + PLUGIN_LIB + File.separator + "commons-codec.jar")
def mqsiprofile = props['mqsiprofile'] ? props['mqsiprofile'].trim() : ""
def env = props['env']
def groovyHome = System.getProperty("groovy.home")
def groovyExe = groovyHome + File.separator + "bin" + File.separator + (isWindows ? "groovy.bat" : "groovy")
def version = props['version'] ? props['version'].trim() : ""
def cmdArgs

if (env) {
    env = env.split("\n|,")

    for (def envArg : env) {
        if(envArg.trim() && envArg.contains('=')) {
            def (key, val) = envArg.trim().split('=', 2)  // split by first occurrence
            println("[Action] Setting environment variable ${key}=${val}")
            helper.addEnvironmentVariable(key, val)
        }
        else {
            println("[Error] Missing a delimiter '=' for environment variable definition : ${envArg}")
        }
    }
}

// append required jar files to classpath
def jarDir = new File(jarPath)
def requiredJars = []

version = Integer.parseInt(version.split("\\.")[0])

if (version < 10) {
    requiredJars << "ConfigManagerProxy"

    if (!argScript.contains("set_bar_props.groovy")) {
        requiredJars.addAll([
            "connector",
            "ibmjsseprovider2",
            "com.ibm.mq",
            "com.ibm.mq.commonservices",
            "com.ibm.mq.headers",
            "com.ibm.mq.jmqi",
            "com.ibm.mq.pcf"
        ])
    }
}
else {
    requiredJars << "IntegrationAPI"
}

for (def jarEntry : jarPath.split(File.pathSeparator)) {
    def jarFile = new File(jarEntry.trim())

    if (jarFile.isDirectory() && requiredJars) {
        def regexPattern = ""
        def regexArr = []
        for (def jar : requiredJars) {
            regexArr << ".*${jar}[^\\" + File.separator + "]*.jar\$"
        }
        regexPattern = StringUtils.join(regexArr, '|')
        def filePattern = Pattern.compile(regexPattern)

        def buildClassPath = {
            if (filePattern.matcher(it.name).find()) {
                def jarName = it.name
                requiredJars.remove(jarName.substring(0, jarName.lastIndexOf('.')))
                classpath.append(File.pathSeparator + it.absolutePath)
            }
        }

        jarFile.eachFileRecurse(buildClassPath)
    }
    else if (jarFile.isFile()) {
        def jarName = jarFile.name
        requiredJars.remove(jarName.substring(0, jarName.lastIndexOf('.')))
        classpath.append(File.pathSeparator + jarFile.absolutePath)
    }
    else {
        println("[Warning] ${jarFile} is not a file or directory on the file system, and it will be ignored.")
    }
}

if (requiredJars) {
    println("[Warning] the following jar files were not found on the Jar Path and are required with this version " +
            "of IIB: '${requiredJars}' Some steps may fail.")
}


if (mqsiprofile) {
    mqsiprofile = new File(mqsiprofile)

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
        helper.runCommand("[Action] Setting up mqsi environment...", cmdArgs, envSet)

        for (def entry : envMap) {
            helper.addEnvironmentVariable(entry.key, entry.value)
        }

        // run script regardless of whether environment was set
        cmdArgs = [
            groovyExe,
            "-cp",
            "${classpath}",
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
            ". ${mqsiprofile.absolutePath} && ${groovyExe} -cp ${classpath.toString()} " +
            "${argScript} ${this.args[1]} ${this.args[2]}"
        ]
    }
}
else {
    cmdArgs = [
        groovyExe,
        "-cp",
        "${classpath}",
        argScript,
        this.args[1],
        this.args[2]
    ]
}
if(apTool.getEncKey() != null) {
    helper.addEnvironmentVariable("UCD_SECRET_VAR", apTool.getEncKey())
}
helper.runCommand(cmdArgs.join(' '), cmdArgs)
