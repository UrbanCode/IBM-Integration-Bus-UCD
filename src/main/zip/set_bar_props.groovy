
/*
 * Licensed Materials - Property of IBM Corp.
 * IBM UrbanCode Build
 * IBM UrbanCode Deploy
 * IBM UrbanCode Release
 * IBM AnthillPro
 * (c) Copyright IBM Corporation 2015. All Rights Reserved.
 *
 * U.S. Government Users Restricted Rights - Use, duplication or disclosure restricted by
 * GSA ADP Schedule Contract with IBM Corp.
 */
import com.urbancode.air.plugin.wmbcmp.BarHelper
import com.urbancode.air.plugin.PropertiesHelper
import com.urbancode.air.FileSet

File workDir = new File('.')
BarHelper helper

Properties props = PropertiesHelper.getProperties(new File(args[0]))
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