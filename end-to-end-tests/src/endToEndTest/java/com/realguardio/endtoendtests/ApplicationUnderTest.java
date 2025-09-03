package com.realguardio.endtoendtests;

import org.springframework.util.ClassUtils;

import java.lang.reflect.InvocationTargetException;

public interface ApplicationUnderTest {

  public static ApplicationUnderTest make() {
    try {
      String className = ApplicationUnderTest.class.getName() + "Using" + System.getProperty("endToEndTestMode", "TestContainers");
      Class<?> clazz = ClassUtils.forName(className, ApplicationUnderTest.class.getClassLoader());
      return (ApplicationUnderTest) clazz. getDeclaredConstructor().newInstance();
    } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | NoSuchMethodException |
             InvocationTargetException e) {
      throw new RuntimeException(e);
    }
  }

  void start();

  int getCustomerServicePort();

  int getOrchestrationServicePort();

  int getSecurityServicePort();

  int getIamPort();

  String iamServiceHostAndPort();
}
