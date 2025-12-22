package io.realguardio.osointegration.ososervice;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;

public class RealGuardOsoAuthorizer {

  private static final Logger logger = LoggerFactory.getLogger(RealGuardOsoAuthorizer.class);

  private final OsoService osoService;

  public RealGuardOsoAuthorizer(OsoService osoService) {
    this.osoService = osoService;
  }

  @CircuitBreaker(name = "osoAuthorizer")
  @TimeLimiter(name = "osoAuthorizer")
  @Retry(name = "osoAuthorizer", fallbackMethod = "isAuthorizedFallback")
  public CompletableFuture<Boolean> isAuthorized(String user, String action, String securitySystem) {
        return CompletableFuture.supplyAsync(() -> osoService.authorize("CustomerEmployee", user, action, "SecuritySystem", securitySystem));
  }

  private CompletableFuture<Boolean> isAuthorizedFallback(String user, String action, String securitySystem, Exception exception) {
    logger.error("isAuthorizedFallback: Authorization service unavailable for user {} attempting action {} on security system {}. Denying access.",
        user, action, securitySystem, exception);
    return CompletableFuture.completedFuture(false);
  }

  public String listLocal(String userId, String action, String resourceType, String column) {
      return osoService.listLocal("CustomerEmployee", userId, action, resourceType, column);
  }
}
