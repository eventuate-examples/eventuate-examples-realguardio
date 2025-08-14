package io.eventuate.examples.realguardio.customerservice.restapi;

import java.lang.reflect.Field;

public class EntityUtil {
  static <T> void setId(T entity, Long id) {
    try {
      Field idField = entity.getClass().getDeclaredField("id");
      idField.setAccessible(true);
      idField.set(entity, id);
    } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
      throw new RuntimeException(e);
    }
  }
}
