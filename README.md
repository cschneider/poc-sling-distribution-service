# Poc for replication as a service with OSGi

This PoC shows how to create sling distribution as a fully cloud native service using OSGi open source building blocks from felix, Aries and sling.

This PoC is built on:

- OSGi
- Aries Jaxrs Whiteboard
- Dropwizard metrics, Sling commons metrics
- Swagger
- Halbrowser
- Felix healthchecks

Docs

- [See here for the high level architecture of the service](https://wiki.corp.adobe.com/display/WEM/Replication+as+a+service).
- [Service API](https://git.corp.adobe.com/content-distribution/raas-api)

## Build

mvn clean install

# Run

java -jar target/

## Try it

[Welcome page](http://localhost:8080)


## Distribute a package

    curl -F pkgId=1001 -F reqType=ADD -F paths=path1,path2 -F content-package=@content-package.zip http://localhost:8080/distribution/queues/stage

This currently will only display the meta data and store the package on disk.

## What is missing

- Service impl is just a dummy
- Swagger UI integration does not yet work
