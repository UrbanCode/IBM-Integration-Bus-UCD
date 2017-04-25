/**
 * (c) Copyright IBM Corporation 2015, 2017.
 * This is licensed under the following license.
 * The Eclipse Public 1.0 License (http://www.eclipse.org/legal/epl-v10.html)
 * U.S. Government Users Restricted Rights:  Use, duplication or disclosure restricted by GSA ADP Schedule Contract with IBM Corp.
 */
import com.urbancode.air.FileSet
import com.urbancode.air.plugin.wmbcmp.BarHelper

File workDir = new File('.')
BarHelper helper

AirPluginTool apTool = new AirPluginTool(this.args[0], this.args[1])
def props = apTool.getStepProperties(System.getenv("UCD_SECRET_VAR"))
def includes = props['includes']
def excludes = props['excludes']

//create list of files from includes and excludes
parseFileList = {
    FileSet fs = new FileSet(workDir)
    def fileIncludes  = includes.split("\n")
    def fileExcludes = excludes.split ("\n")

    for (file in fileIncludes) {
        fs.include(file.trim())
    }

    for (file in fileExcludes) {
        fs.exclude(file.trim())
    }

    return fs.files()
}

def barFileList = parseFileList()
helper = new BarHelper(barFileList, props)
helper.overrideProperties()
