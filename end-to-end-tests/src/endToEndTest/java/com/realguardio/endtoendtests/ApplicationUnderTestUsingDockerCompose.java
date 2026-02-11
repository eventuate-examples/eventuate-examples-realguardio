package com.realguardio.endtoendtests;

public class ApplicationUnderTestUsingDockerCompose implements ApplicationUnderTest {

  @SuppressWarnings("unused")
  public ApplicationUnderTestUsingDockerCompose(String networkName) {
    // networkName is ignored for DockerCompose mode
  }

  @Override
  public void start() {

  }

  @Override
  public int getCustomerServicePort() {
    return 3002;
  }

  @Override
  public int getOrchestrationServicePort() {
    return 3003;
  }

  @Override
  public int getSecurityServicePort() {
    return 3001;
  }

  @Override
  public int getIamPort() {
    return 9000;
  }

  @Override
  public String iamServiceHostAndPort() {
    return "realguardio-iam-service:" + getIamPort();
  }
}
