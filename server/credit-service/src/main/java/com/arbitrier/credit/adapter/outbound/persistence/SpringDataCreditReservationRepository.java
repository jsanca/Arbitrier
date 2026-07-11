package com.arbitrier.credit.adapter.outbound.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data JPA repository for {@link CreditReservationEntity}.
 *
 * <p>Used exclusively by {@link JpaCreditReservationRepositoryAdapter}.
 *
 * <p>Layer: adapter/outbound/persistence
 * <p>Module: credit-service
 */
public interface SpringDataCreditReservationRepository
        extends JpaRepository<CreditReservationEntity, String> {
}
