# jenkins-plugin

Run ExplorViz as a build step or pipeline stage in Jenkins CI.

## Building

You need to have a local Maven 3 and JDK 8 or 11 installation, then simply run `mvn package`. This will create the `target/explorviz-plugin.hpi` file.

*TODO:* Support building on Travis

### On Jenkins CI

A `Jenkinsfile` is included to build this plugin on Jenkins CI with several JDK versions and Jenkins versions in parallel, however the setup is more complex and a Linux host is required right now:

* You need to install the [jenkins-infra/pipeline-library](https://github.com/jenkins-infra/pipeline-library) repository as a global pipeline library in `Manage Jenkins -> Configure System -> Global Pipeline Libraries`
* The project has to be of type `Multibranch Pipeline`.
* You need to label (one of) your jenkins node(s) with the tag `linux`.
* In `Global Tool Configuration` you need to configure a JDK 8 installation with the name `jdk8` and a JDK 11 installation with the name `jdk11`.
* You also need a Maven 3 installation with the name `mvn`.

## Running

Install the `explorviz-plugin.hpi` file in Jenkins under `Manage Jenkins -> Manage Plugins -> Advanced -> Upload Plugin`. Only an administrator can do this.

The minimum supported Jenkins version is 2.150.1, but it is recommended to always run the latest Jenkins (LTS) version.
