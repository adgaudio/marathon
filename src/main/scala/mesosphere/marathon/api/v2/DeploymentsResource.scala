package mesosphere.marathon.api.v2

import javax.inject.Inject
import javax.ws.rs._
import javax.ws.rs.core.MediaType

import mesosphere.marathon.api.RestResource
import mesosphere.marathon.state.GroupManager
import mesosphere.marathon.upgrade.{ DeploymentAction, DeploymentStep, DeploymentPlan }
import mesosphere.marathon.{ MarathonConf, MarathonSchedulerService }

@Path("v2/deployments")
@Produces(Array(MediaType.APPLICATION_JSON))
class DeploymentsResource @Inject() (service: MarathonSchedulerService, groupManager: GroupManager, val config: MarathonConf) extends RestResource {

  @GET
  def running() = ok(result(service.listRunningDeployments()).map {
    case (plan, currentStep) => toInfo(plan, currentStep)
  })

  @DELETE
  @Path("{id}")
  def cancel(@PathParam("id") id: String) = {
    result(service.listRunningDeployments()).find(_._1.id == id).fold(notFound(s"DeploymentPlan $id does not exist")) {
      case (plan, _) =>
        deploymentResult(result(groupManager.update(plan.original.id, _ => plan.original, force = true)))
    }
  }

  private def toInfo(deployment: DeploymentPlan, currentStep: DeploymentStep) = Map(
    "id" -> deployment.id,
    "version" -> deployment.version,
    "affectedApplications" -> deployment.affectedApplicationIds.map(_.toString),
    "steps" -> deployment.steps.map(step => step.actions.map(actionToMap)),
    "currentActions" -> currentStep.actions.map(actionToMap)
  )

  def actionToMap(action: DeploymentAction) =
    Map(
      "action" -> action.getClass.getSimpleName,
      "application" -> action.app.id.toString
    )
}
