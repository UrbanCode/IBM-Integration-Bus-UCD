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
 import com.urbancode.air.AirPluginTool;
 import com.urbancode.air.plugin.wmbcmp.IIBHelper;

 File workDir = new File('.');

 AirPluginTool apTool = new AirPluginTool(this.args[0], this.args[1])
 def props = apTool.getStepProperties(System.getenv("UCD_SECRET_VAR"))
 def helper = new IIBHelper(props)

 String executionGroup = props['executionGroup']
 def executionGroups
 if(executionGroup != null && !executionGroup.trim().isEmpty()) {
     executionGroups = executionGroup.split('\n|,')*.trim()
     executionGroups -= [null, ""] // remove empty and null entries
 }

 try {
     for (String groupName : executionGroups) {
         helper.restartExecutionGroup(groupName);
     }
 }
 catch (Exception e) {
     e.printStackTrace();
     throw e;
 }
 finally {
     helper.cleanUp()
 }
