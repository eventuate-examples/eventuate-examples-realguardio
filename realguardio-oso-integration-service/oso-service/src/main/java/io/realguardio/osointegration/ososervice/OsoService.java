package io.realguardio.osointegration.ososervice;

import com.osohq.oso_cloud.ApiException;
import com.osohq.oso_cloud.Fact;
import com.osohq.oso_cloud.Oso;
import com.osohq.oso_cloud.Value;

import java.io.IOException;

public class OsoService {

  private final Oso oso;

  public OsoService(Oso oso) {
    this.oso = oso;
  }

  public void createRole(String actorType, String actorId, String role, String resourceType, String resourceId) {
    try {
      oso.insert(new Fact(
              "has_role",
              new Value(actorType, actorId),
              new Value(role),
              new Value(resourceType, resourceId)
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

    public String listLocal(String actorType, String actorId, String action, String resourceType, String column) {
        try {
            return oso.listLocal(new Value(actorType, actorId), action, resourceType, column);
        } catch (IOException | ApiException e) {
            throw new RuntimeException(e);
        }
    }

    public String authorizeLocal(String actorType, String actorId, String action, String resourceType, String resourceId) {
        try {
            return oso.authorizeLocal(
                    new Value(actorType, actorId),
                    action,
                    new Value(resourceType, resourceId)
            );
        } catch (IOException | ApiException e) {
            throw new RuntimeException(e);
        }
    }
}
