# OctopusDeploy Jenkins Plugin #
Connecting [OctopusDeploy](https://octopusdeploy.com/) to the [Jenkins](https://jenkins-ci.org/) workflow. Integration with OctopusDeploy is achieved via the REST API, not the Octo.exe tool.

## Compatibility ##
Fully compatible with OctopusDeploy 2.6.

Tested against OctopusDeploy 3.0 - 3.7.

# Main Components #
## Post-build step - Create Release ##
Creates a new release entry for a given project.
Optionally also deploys the newly created release to a given environment.

This component will only run if the build has been successful up until the point the post-build step is invoked.

## Post-build step - Execute Deployment ##
Sends a release to a given environment.

This component will only run if the build has been successful up until the point the post-build step is invoked.

# Features #
## REST API Integration ##
This plugin implements part of the REST API for OctopusDeploy in order to retrieve data and execute commands on the server. 
It makes use of an API Key which is configured in the Global Settings Jenkins configuration page. This API Key is used for all interactions with OctopusDeploy.

## Environment Variable Support ##
Environment variables can be used in many fields in the Release and Deployment components of this plugin. Any variable, written as ${VARIABLE_NAME} will get replaced by that variable's value. If there is no value set for that variable, the token is not replaced.

## Custom Release Notes ##
The Release component allows Release Notes to be gathered from SCM, collecting commit messages from the current build all the way back to the last successful build, or it can load in Release Notes from a file.

There is also an option to provide a link back to the job run that created the release in the Release Notes, allowing easy navigation between Jenkins jobs and OctopusDeploy Releases.

## Build Summary Links ##
Each component provides a link to the Release or Deployment that it created. These links are provided in the console output of the job as well as showing up as a build badge, and in the Build Summary.

## Wait-for-Deployment ##
The Deployment component can optionally wait for the completion of the Deployment, and record the status. If the status returns as failed, the Jenkins job will be marked as a failure.

## Autocomplete ##
Some entry fields, like Project and Environment support auto-completion, pulling a list of names to choose from from the OctopusDeploy server.

## Octopus variables ##
As of 1.4.0, this plugin can set Octopus variables for use in deployment.

## Multi tenant support ##
As of 1.5.0, this plugin can submit a Tenant to the deployment step for use in Octopus' multi-tenant mode.