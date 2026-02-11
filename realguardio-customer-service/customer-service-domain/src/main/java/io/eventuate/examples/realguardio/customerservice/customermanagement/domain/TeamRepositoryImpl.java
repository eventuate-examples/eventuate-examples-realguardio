package io.eventuate.examples.realguardio.customerservice.customermanagement.domain;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

@Repository
public class TeamRepositoryImpl implements TeamRepositoryCustom {

  @PersistenceContext
  private EntityManager entityManager;

  @Override
  public Team findRequiredById(long teamId) {
    Team team = entityManager.find(Team.class, teamId);
    if (team == null) {
      throw new EntityNotFoundException("Team not found with id: " + teamId);
    }
    return team;
  }
}