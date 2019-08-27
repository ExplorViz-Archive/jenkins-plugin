# jenkins-plugin

Use ExplorViz to visualize software landscapes of each build from within Jenkins CI.

## Building

You need to have a local Maven 3 and JDK 8 or 11 installation, then simply run `mvn package`.
This will create the `target/explorviz-plugin.hpi` file.

*TODO: Support building on Travis*

### On Jenkins CI

A `Jenkinsfile` is included to build this plugin on Jenkins CI with several JDK versions and Jenkins versions in
parallel, however the setup is more complex and a Linux host is required right now:

* You need to install the [jenkins-infra/pipeline-library](https://github.com/jenkins-infra/pipeline-library)
  repository as a global pipeline library in `Manage Jenkins -> Configure System -> Global Pipeline Libraries`
* You need to label (one of) your jenkins node(s) with the tag `linux`.
* In `Global Tool Configuration` you need to configure a JDK 8 installation with the name `jdk8` and a
  JDK 11 installation with the name `jdk11`.
* You also need a Maven 3 installation with the name `mvn`.
* The project for this plugin needs to be created as type `Multibranch Pipeline`.

## Installation

To run the plugin in production, install the `explorviz-plugin.hpi` file in Jenkins under
`Manage Jenkins -> Manage Plugins -> Advanced -> Upload Plugin`. Only an administrator can do this.

The minimum supported Jenkins version is 2.150.3, but it is recommended to always run the latest Jenkins (LTS) version.

## Usage

To visualize builds with ExplorViz, the `Run Kieker instrumentation for ExplorViz` build step needs to be run during the build.
This step can be used multiple times to instrument multiple applications/scenarios in one build.

In FreeStyle-type projects, this build step can be added in the `Build` section.
In Pipeline-based projects, use the `kieker` pipeline command. A full command-line, including the available parameters,
can be generated using the Pipeline Snippet Generator integrated into Jenkins.

When the instrumentation succeeds, a menu entry `ExplorViz Visualization` is added to the build's page.

## Development

To debug and test the plugin when developing, run `mvn hpi:run`.
A local jenkins instance will be started on port `8080` with the plugin installed.
See the jenkins plugin development documentation for more information.
