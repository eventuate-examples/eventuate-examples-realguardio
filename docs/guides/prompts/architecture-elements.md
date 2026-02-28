## Static architecture:

* This application has a microservice architecture.
* Each service consists of subdomains
* A subdomain has an hexagonal architecture and consists of modules (domain, adapters - inbound/outbound, etc)
* Modules consist of classes and other artifacts/files

In addition to the services, there are

* infrastructure services: Kafka, databases, IAM services
* external cloud-based services

## Collaboration

Services collaborate via APIs:
* Synchronously
** REST APIs for synchronous communication
* Asynchronously 
** Publish/subscribe
** Request/async-response


Classes within a service collaborate via method calls