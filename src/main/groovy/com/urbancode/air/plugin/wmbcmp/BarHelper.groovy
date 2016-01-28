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
package com.urbancode.air.plugin.wmbcmp

import com.ibm.broker.config.proxy.BarFile
import com.ibm.broker.config.proxy.DeploymentDescriptor

class BarHelper {
    def workDir = new File('.')
    def barFiles
    def properties
    def propFile
    def onFail
    def propCount = 0
    HashMap propMap

    BarHelper(def barFiles, def props) {
        propMap = new HashMap<String, String>()
        this.barFiles = barFiles
        properties = props['properties']
        onFail = props['onFail']
        parseProperties()
    }

    //parse properties file or properties list and put results in map
    private void parseProperties () {
        def unparsedProps

        if (!properties) {
            throw new Exception("Must specify either a properties file or individual properties separated by new lines.")
        }
        //allow user to pass a property file or individual properties
        else {
            propFile = new File(properties)

            if (propFile.exists() && propFile.isFile()) {
                unparsedProps = propFile.text
            }
            else {
                propFile = new File(workDir, properties)

                if (propFile.exists() && propFile.isFile()) {
                    unparsedProps = propFile.text
                }
                else {
                    propFile = null
                    unparsedProps = properties
                }
            }
        }

        //split each line into key/value pairs separated by =
        for (line in unparsedProps.split ('\n')) {
            if (line.contains('=')) {
                String[] entries = line.split ('=')
                String name = entries[0].trim()
                String value = entries[1].trim()
                propMap.put(name, value)
                propCount++
            }
            else if (line) {
                throw new Exception("${line} is not a valid property definition. Property definitions must include '=' as a delimiter.")
            }
        }
    }

    //override specified properties in all specified bar files
    void overrideProperties() {
        def failures = []

        for (bar in barFiles) {
            try {
                if (!BarFile.isValid(bar.absolutePath)) {
                    if (onFailure == "fastFail") {
                        println("Property overrides failed on bar file: ${bar.absolutePath} (Invalid bar file) Process will now exit.")
                        System.exit(1)
                    }
                    else if (onFailure == "Best Effort") {
                        failures << barFile.absolutePath
                        continue
                    }
                    else {
                        println("Property overrides failed on bar file: ${bar.absolutePath} (Invalid bar file)")
                        continue
                    }
                }

                println("Overriding ${propCount} propert" + (propCount > 1 ? "ies" : "y") + " on file : ${bar.absolutePath}")

                if (propFile) {
                    println("Property file used for override: ${propFile.absolutePath}")
                }

                BarFile barFile = BarFile.loadBarFile(bar.absolutePath)
                //application and library name left null, so all applications and libraries in the file will be affected
                //recursion true, recursing through any nested applications/libraries
                barFile.applyOverrides(propMap, null, null, true)
                barFile.save()

                println("Bar property overrides successful.")

                //print properties on bar file that are being successfully overridden
                DeploymentDescriptor descriptor = barFile.getDeploymentDescriptor()

                //the deployment descriptor file doesn't exist or cannot be accessed within the bar file
                if (!descriptor) {
                    println("Warning: Cannot access deployment descriptor file (broker.xml) for bar file: ${bar.absolutePath}")
                    println("Unable to output contents of the deployment descriptor.")
                }
                else {
                    def overrides = descriptor.getOverriddenPropertyIdentifiers()
                    println("Currently overridden properties on bar file (${bar.absolutePath}):")
                    overrides.each {
                        println(it)
                    }
                }
            }
            catch(Exception ex) {
                println("Exception thrown when applying property overrides on file : ${bar.absolutePath}.")
                ex.printStackTrace()
                throw ex
            }
        }

        //best effort on failure selection reports failures and exits after all bar files are attempted
        if (failures) {
            for (fail in failures) {
                println("Property overrides failed on bar file: ${bar.absolutePath} (Invalid bar file)")
            }

            if (propFile) {
                println("Property file used for attempted overrides: ${propFile.absolutePath}")
            }

            println("Process will now exit due to errors.")
            System.exit(1)
        }
    }
}