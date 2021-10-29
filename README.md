# Nexus Repository Cargo Format

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.sonatype.nexus.plugins/nexus-repository-cargo/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.sonatype.nexus.plugins/nexus-repository-cargo)
[![CircleCI](https://circleci.com/gh/sonatype-nexus-community/nexus-repository-cargo.svg?style=shield)](https://circleci.com/gh/sonatype-nexus-community/nexus-repository-cargo)
[![Join the chat at https://gitter.im/sonatype/nexus-developers](https://badges.gitter.im/sonatype/nexus-developers.svg)](https://gitter.im/sonatype/nexus-developers?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)
[![DepShield Badge](https://depshield.sonatype.org/badges/sonatype-nexus-community/nexus-repository-cargo/depshield.svg)](https://depshield.github.io)

## Developing

### Requirements

* [Apache Maven 3.3.3+](https://maven.apache.org/install.html)
* [Java 8](http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html)
* Network access to https://repository.sonatype.org/content/groups/sonatype-public-grid

Also, there is a good amount of information available at [Bundle Development](https://help.sonatype.com/display/NXRM3/Bundle+Development)

### Building

To build the project and generate the bundle use Maven

    mvn clean package

If everything checks out, the bundle for cargo should be available in the `target` folder

#### Build with Docker

`docker build -t nexus-repository-cargo .`

#### Run as a Docker container

`docker run -d -p 8081:8081 --name nexus-repository-cargo nexus-repository-cargo`

For further information like how to persist volumes check out [the GitHub repo for our official image](https://github.com/sonatype/docker-nexus3).

After allowing some time to spin up, the application will be available from your browser at http://localhost:8081.

To read the generated admin password for your first login to the web UI, you can use the command below against the running docker container:

    docker exec -it nexus-repository-cargo cat /nexus-data/admin.password && echo

For simplicity, you should check `Enable anonymous access` in the prompts following your first login.

## Using Cargo With Nexus Repository Manager 3

[We have detailed instructions on how to get started here!](docs/CARGO_USER_DOCUMENTATION.md)

## Compatibility with Nexus Repository Manager 3 Versions

The table below outlines what version of Nexus Repository the plugin was built against

| Plugin Version | Nexus Repository Version |
|----------------|--------------------------|
| v0.0.1         | 3.12.0-01                |
| v0.0.1         | 3.29.2-02                |
| v0.0.2         | 3.30.1-01                |
| v0.0.3         | 3.30.1-01                |
| v0.0.4         | 3.36.0-01                |

If a new version of Nexus Repository is released and the plugin needs changes, a new release will be made, and this
table will be updated to indicate which version of Nexus Repository it will function against. This is done on a time 
available basis, as this is community supported. If you see a new version of Nexus Repository, go ahead and update the
plugin and send us a PR after testing it out!

All released versions can be found [here](https://github.com/sonatype-nexus-community/nexus-repository-cargo/releases).

## Features Implemented In This Plugin 

| Feature | Implemented          |
|---------|----------------------|
| Proxy   |                      |
| Hosted  |                      |
| Group   |                      |

### Supported Cargo Commands

#### Proxy

Command | Plugin Version               | Implemented              |
--------|------------------------------|--------------------------|
login   |                              |                          |
search  |                              |                          |
fetch   |                              |                          |
publish |                              |                          |
yank    |                              |                          |

#### Hosted

Command | Plugin Version               | Implemented              |
--------|------------------------------|--------------------------|
login   |                              |                          |
search  |                              |                          |
fetch   |                              |                          |
publish |                              |                          |
yank    |                              |                          |

## Installing the plugin

There are a range of options for installing the cargo plugin. You'll need to build it first, and
then install the plugin with the options shown below:

### Temporary Install

Installations done via the Karaf console will be wiped out with every restart of Nexus Repository. This is a
good installation path if you are just testing or doing development on the plugin.

* Enable Nexus Repo console: edit `<nexus_dir>/bin/nexus.vmoptions` and change `karaf.startLocalConsole`  to `true`.

  More details here: [Bundle Development](https://help.sonatype.com/display/NXRM3/Bundle+Development+Overview)

* Run Nexus Repo console:
  ```
  # sudo su - nexus
  $ cd <nexus_dir>/bin
  $ ./nexus run
  > bundle:install file:///tmp/nexus-repository-cargo-0.0.1.jar
  > bundle:list
  ```
  (look for org.sonatype.nexus.plugins:nexus-repository-cargo ID, should be the last one)
  ```
  > bundle:start <org.sonatype.nexus.plugins:nexus-repository-cargo ID>
  ```

### (more) Permanent Install

For more permanent installs of the nexus-repository-cargo plugin, follow these instructions:

* Copy the bundle (nexus-repository-cargo-0.0.1.jar) into <nexus_dir>/deploy

This will cause the plugin to be loaded with each restart of Nexus Repository. As well, this folder is monitored
by Nexus Repository and the plugin should load within 60 seconds of being copied there if Nexus Repository
is running. You will still need to start the bundle using the karaf commands mentioned in the temporary install.

### (most) Permanent Install

If you are trying to use the cargo plugin permanently, it likely makes more sense to do the following:

* Copy the bundle into `<nexus_dir>/system/org/sonatype/nexus/plugins/nexus-repository-cargo/0.0.1/nexus-repository-cargo-0.0.1.jar`
* Make the following additions marked with + to `<nexus_dir>/system/org/sonatype/nexus/assemblies/nexus-core-feature/3.x.y/nexus-core-feature-3.x.y-features.xml`

   ```
         <feature prerequisite="false" dependency="false">wrap</feature>
   +     <feature prerequisite="false" dependency="false">nexus-repository-cargo</feature>
     </feature>
   ```
   to the `<feature name="nexus-core-feature" description="org.sonatype.nexus.assemblies:nexus-core-feature" version="3.x.y.xy">` section below the last (above is an example, the exact last one may vary).
   
   And
   ```
   + <feature name="nexus-repository-cargo" description="org.sonatype.nexus.plugins:nexus-repository-cargo" version="0.0.1">
   +     <details>org.sonatype.nexus.plugins:nexus-repository-cargo</details>
   +     <bundle>mvn:org.sonatype.nexus.plugins/nexus-repository-cargo/0.0.1</bundle>
   + </feature>
    </features>
   ```
   as the last feature.
   
This will cause the plugin to be loaded and started with each startup of Nexus Repository.

## The Fine Print

It is worth noting that this is **NOT SUPPORTED** by Sonatype, and is a contribution of ours and Imperva to the open source community (read: you!)

Remember:

* Use this contribution at the risk tolerance that you have
* Do NOT file Sonatype support tickets related to cargo support in regard to this plugin
* DO file issues here on GitHub, so that the community can pitch in

Phew, that was easier than I thought. Last but not least of all:

Have fun creating and using this plugin and the Nexus platform, we are glad to have you here!

## Getting help

Looking to contribute to our code but need some help? There's a few ways to get information:

* Chat with us on [Gitter](https://gitter.im/sonatype/nexus-developers)
* Check out the [Nexus3](http://stackoverflow.com/questions/tagged/nexus3) tag on Stack Overflow
* Check out the [Nexus Repository User List](https://groups.google.com/a/glists.sonatype.com/forum/?hl=en#!forum/nexus-users)
