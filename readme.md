# OctopusDeploy Jenkins Plugin #
Connecting [OctopusDeploy](https://octopusdeploy.com/) to the [Jenkins](https://jenkins-ci.org/) workflow. Integration with OctopusDeploy is achieved via the REST API,
not the Octo.exe tool.

## Compatibility ##
Tested and compatible with OctopusDeploy: 2.6 and 3.0 - 3.7.

Tested and compatible with Octopus 2018.9 as of 1.9.0 of this plugin.

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
Environment variables can be used in many fields in the Release and Deployment components of this plugin. Any variable, written as ${VARIABLE_NAME} will get replaced
by that variable's value. If there is no value set for that variable, the token is not replaced.

## Custom Release Notes ##
The Release component allows Release Notes to be gathered from SCM, collecting commit messages from the current build all the way back to the last successful build,
or it can load in Release Notes from a file.

There is also an option to provide a link back to the job run that created the release in the Release Notes, allowing easy navigation between Jenkins jobs and
OctopusDeploy Releases.

## Build Summary Links ##
Each component provides a link to the Release or Deployment that it created. These links are provided in the console output of the job as well as showing up as a build
badge, and in the Build Summary.

## Wait-for-Deployment ##
The Deployment component can optionally wait for the completion of the Deployment, and record the status. If the status returns as failed, the Jenkins job will be marked
as a failure.

## Autocomplete ##
Some entry fields, like Project and Environment support auto-completion, pulling a list of names to choose from from the OctopusDeploy server.

## Octopus variables ##
As of 1.4.0, this plugin can set Octopus variables for use in deployment.

## Multi tenant support ##
As of 1.5.0, this plugin can submit a Tenant to the deployment step for use in Octopus' multi-tenant mode.

## Creating release on specific Channels ##
As of 1.6.0, this plugin can create releases on specific Channels as defined by users.

## Multiple Octopus servers ##
As of 1.7.0, this plugin now allows more than one Octopus server to be configured in the global Jenkins configuration. The selection of which Octopus server to use will be
done by the plugin on a per-project basis (under Advanced Options). Note that unless otherwise specified, each project will use the first Octopus server listed.

# Debugging

To debug the plugin you need Java JDK 8 installed, you can download it from [here](https://jdk.java.net/java-se-ri/8)

## IntelliJ configuration

- Maven configuration
  - Parameters tab
    - Command line field: `hpi:run`
  - If you want to run Jenkins on another port than the default **8080**
    - Runner tab
     - Add `-Djetty.port=<PortToUse>` to the VM options field
- Open browser to http://localhost:8080/jenkins

### If you want to connect to your Octopus instance using `https`

- Update your Maven configuration in IntelliJ
  - Runner tab
    - Add the following options to the VM options field:
      - `-Djavax.net.ssl.trustStore="%JAVA_HOME%/jre/lib/security/cacerts"` (You _might_ need to unfurl the `%JAVA_HOME%` environment variable to the absolute path of your JDK installation)
      - `-Djavax.net.ssl.trustStorePassword=changeit`
- Save the certificate for your Octopus Server as a `Base-64 encoded X.509` file
  - Run `& '$env:JAVA_HOME/bin/keytool.exe' -import -alias <GiveItAnAlias> -keystore $env:JAVA_HOME/jre/lib/security/cacerts -file <PathToTheCertFile>`
    - Enter the password for your keystore (by default it's `changeit`)
    - Say `yes` to the question if you want to trust the certificate

## Troubleshooting

### Error - trustAnchors parameter must be non-empty

You haven't configured SSL trust store correctly, see [If you want to connect to your Octopus instance using `https`](#if-you-want-to-connect-to-your-octopus-instance-using-https).

### WARNING: Header is too large >8192

If you restart the Jenkins service too often you may be greeted by a bunch of 413 HTTP error messages, to get rid of this error just clear all cookies for your Jenkins site (http://localhost:8080 by default). More information can be found [here](https://issues.jenkins-ci.org/browse/JENKINS-25046)
