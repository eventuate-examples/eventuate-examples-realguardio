package io.realguardio.osointegration.ososervice;

import com.osohq.oso_cloud.ApiException;
import com.osohq.oso_cloud.Fact;
import com.osohq.oso_cloud.Oso;
import com.osohq.oso_cloud.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class OsoService {

  private final Oso oso;

  public OsoService(Oso oso) {
    this.oso = oso;
  }

  public void createRoleForCustomerEmployeeAtCustomer(String customerEmployeeId, String customerID, String role) {
    try {
      oso.insert(new Fact(
              "has_role",
              new Value("CustomerEmployee", customerEmployeeId),
              new Value(role),
              new Value("Customer", customerID)
      ));
    } catch (IOException | ApiException e) {
      throw new RuntimeException(e);
    }
  }

  public void createRelation(String resourceType, String resourceId, String relation, String targetType, String targetId) {
    try {
      oso.insert(new Fact(
              "has_relation",
              new Value(resourceType, resourceId),
              new Value(relation),
              new Value(targetType, targetId)
      ));
    } catch (IOException | ApiException e) {
      throw new RuntimeException(e);
    }
  }

  public boolean authorize(String actorType, String actorId, String action, String resourceType, String resourceId) {
    try {
      return oso.authorize(
              new Value(actorType, actorId),
              action,
              new Value(resourceType, resourceId)
      );
    } catch (IOException | ApiException e) {
      throw new RuntimeException(e);
    }
  }
}
