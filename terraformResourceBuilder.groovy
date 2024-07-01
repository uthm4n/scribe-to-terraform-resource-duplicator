import com.morpheusdata.core.util.HttpApiClient
import groovy.json.JsonOutput
import org.w3c.dom.css.Counter
import com.morpheusdata.core.util.MorpheusUtils

Map getTerraformConfig(int workflowId) {
    def client = new HttpApiClient()
    def requestOptions = new HttpApiClient.RequestOptions()
    def apiToken = morpheus.apiAccessToken.size() == 36 ? morpheus.apiAccessToken.toString() : cypher.read('secret/morpheus-api-key')
    requestOptions.headers = ["Authorization": "Bearer ${apiToken}"]
    requestOptions.ignoreSSL = true

    def terraformResource = [:]
    try {
        def response = client.callJsonApi("${morpheus.applianceUrl}", "/api/task-sets/${workflowId}", requestOptions, "GET")
        if (response.success) {
            def workflow = response.data?.taskSet
            terraformResource.workflowOne = [
                    "name": workflow.name + "-cloned",
                    "labels": workflow.labels,
                    "task_ids": workflow.tasks,
                    "option_types": workflow.optionTypes.id
            ]
        }
    } catch (Exception e) {
        println "Failure occurred! ${e.printStackTrace()}"
    }
    return terraformResource
}

def terraformResource = getTerraformConfig(15) // Enter a workflow ID here

String buildTerraformConfig(Map terraformResource) {
    def workflow = terraformResource.workflowOne
    return """
terraform {
  required_providers {
    morpheus = {
      source  = "gomorpheus/morpheus"
      version = "0.9.8"
    }
  }
}

provider "morpheus" {
  url      = "${morpheus.applianceUrl}"
  username = "${username}"
  password = "${password}"
}

resource "morpheus_operational_workflow" "${workflow.name}" {
  name   = "${workflow.name}"
  labels = ${JsonOutput.toJson(workflow.labels)}
  task_ids = ${JsonOutput.toJson(workflow.task_ids)}
  option_types = ${JsonOutput.toJson(workflow.option_types)}
}
"""
}

println buildTerraformConfig(terraformResource)
