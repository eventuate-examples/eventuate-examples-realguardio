## Static architecture:

* This application has a microservice architecture.
* Each service consists of subdomains
* A subdomain has an hexagonal architecture and consists of modules (domain, adapters - inbound/outbound, etc)
* Modules consist of classes and other artifacts/files
* Some services contain library modules that are shared with other services. A class might be defined in one service's source tree but used by another service and invoked locally within that service's process.

Architecturally significant classes include:
* Key classes for integrating with external services (e.g., API client wrappers, resilience wrappers)
* Key domain model classes (e.g., entities, aggregate roots)
* Port interfaces and their implementations

These should be documented in guide text, shown in sequence diagrams, and listed in the Project Structure table.

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