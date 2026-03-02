# JaCoCo Coverage for Component Tests

## Problem

Component tests run the production service inside a Docker container. The JaCoCo agent attached to the Gradle test JVM only instruments the test classes, not the production code running in the container. As a result, component test coverage is missing from JaCoCo reports.

## Solution

Attach the JaCoCo agent to the JVM inside the container and use the agent's TCP server mode to extract execution data after tests complete.

### Steps

#### 1. Add the JaCoCo agent JAR to the container image

Copy the agent into the Docker image:

```dockerfile
# Download JaCoCo agent
ADD https://repo1.maven.org/maven2/org/jacoco/org.jacoco.agent/0.8.12/org.jacoco.agent-0.8.12-runtime.jar /jacoco/jacocoagent.jar
```

#### 2. Configure the container to start with the agent

In the component test, set `JAVA_TOOL_OPTIONS` to attach the agent in TCP server mode:

```java
service.withEnv("JAVA_TOOL_OPTIONS",
    "-javaagent:/jacoco/jacocoagent.jar=output=tcpserver,address=*,port=6300")
    .withExposedPorts(8080, 6300);
```

The agent listens on port 6300 for TCP connections to dump execution data.

#### 3. Extract execution data after tests complete

Add an `@AfterAll` method to dump coverage data from the running container:

```java
@AfterAll
static void dumpCoverage() throws Exception {
    ExecDumpClient client = new ExecDumpClient();
    client.setDump(true);
    client.setReset(true);

    ExecFileLoader loader = client.dump(
        service.getHost(),
        service.getMappedPort(6300)
    );

    loader.save(new File("build/jacoco/componentTest.exec"), true);
}
```

This must run while the container is still alive. Since the `service` container is not managed by `@Testcontainers` (no `@Container` annotation), it remains running during `@AfterAll`.

#### 4. Add the JaCoCo core dependency

```groovy
componentTestImplementation 'org.jacoco:org.jacoco.core:0.8.12'
```

#### 5. Include in aggregate report

The aggregate report plugin already picks up `*.exec` files from `build/jacoco/`, so `componentTest.exec` will be included automatically.

## Key Details

- **TCP vs file dump**: TCP mode is more reliable than file-based dump (`output=file`) because container shutdown can be abrupt, truncating the `.exec` file. TCP allows explicit retrieval while the JVM is still running.
- **`setReset(true)`**: Clears the agent's execution data after dumping, which is useful if you want to dump per-test-class.
- **Container lifecycle**: The `service` container is started manually in `@DynamicPropertySource` and is not stopped by `@Testcontainers`. It stays alive through `@AfterAll`.
