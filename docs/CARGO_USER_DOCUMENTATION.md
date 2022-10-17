<!--
    Copyright 2019, Imperva, Inc. All rights reserved.
    
    Imperva, the Imperva logo, SecureSphere, Incapsula, CounterBreach,
    ThreatRadar, Camouflage, Attack Analytics, Prevoty and design are trademarks
    of Imperva, Inc. and its subsidiaries. All other brand or product names are
    trademarks or registered trademarks of their respective holders.
-->
## Cargo Repositories

### Introduction

Cargo is the [Rust](https://www.rust-lang.org/) package manager. Rust refers to its packages as crates. Cargo is used to download Rust crate dependencies, compile distributable crates, and upload those crates to [crates.io](https://crates.io/) - the Rust community's package registry.

### Proxying Cargo Repositories

You can set up a Cargo proxy repository to access a remote crate index. To proxy a Cargo crate index, you simply create a new _cargo_ proxy as documented in [Repository Management](https://help.sonatype.com/repomanager3/nexus-repository-administration/repository-management#RepositoryManagement-ProxyRepository).

Minimal configuration steps are:
* Define _Name_ e.g. cargo-proxy
* Define URL for _Remote Storage_ e.g. https://crates.io/ 
* Select a Blob Store for _Local Storage_

### Hosting

You can set up a Cargo hosted repository to store your hosted or private crates.

Minimal configuration steps are:
* Define _Name_ e.g. cargo-hosted
* Select a Blob Store for _Local Storage_

#### Publishing Crates
Update your `.cargo/config.toml` file as follows:
```
[net]
git-fetch-with-cli = true

[registries.nexus]
registry = "http://localhost:8081/repository/cargo-hosted/index"
```

Obtain a token for authentication:
`curl -X GET -u <username> http://localhost:8081/repository/cargo-hosted/token`

Copy the token to `.cargo/credentials`:
```
[registries.nexus]
token = "Bearer CargoRegistryBearerToken.<token>"
```

Publish crates to your registry using:
`cargo publish --registry nexus`
or
```
curl \
 -u usr:psw \
 -T example-crate-0.0.1.tar.gz \
 http://localhost:8081/repository/cargo-hosted/example-crate-0.0.1.tar.gz
```

### Configuring Cargo Registries

TBD

### Browsing Cargo Repository Packages

You can browse Cargo repositories in the user interface inspecting the components and assets and their details, as
described in [Browsing Repositories and Repository Groups](https://help.sonatype.com/display/NXRM3/Browsing+Repositories+and+Repository+Groups).
