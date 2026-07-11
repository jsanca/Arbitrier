package com.arbitrier.credit.adapter.outbound.persistence;

import com.arbitrier.credit.domain.model.CreditReservation;
import com.arbitrier.credit.domain.model.CreditReservationId;
import com.arbitrier.credit.domain.model.CreditReservationStatus;
import com.arbitrier.credit.domain.model.Money;
import com.arbitrier.platform.validation.Require;

/**
 * Maps between {@link CreditReservation} domain aggregates and {@link CreditReservationEntity}.
 *
 * <p>Layer: adapter/outbound/persistence
 * <p>Module: credit-service
 */
public class CreditReservationPersistenceMapper {

    /** Creates a new entity from the domain aggregate. */
    public CreditReservationEntity toEntity(CreditReservation reservation) {
        CreditReservationEntity entity = new CreditReservationEntity();
        entity.setId(reservation.id().value());
        entity.setOrderId(reservation.orderId());
        entity.setAmountValue(reservation.amount().amount());
        entity.setAmountCurrency(reservation.amount().currency());
        entity.setStatus(reservation.status().name());
        entity.setVersion(reservation.version());
        return entity;
    }

    /** Updates an existing managed entity with state from the domain aggregate. */
    public CreditReservationEntity updateEntity(CreditReservationEntity existing,
                                                CreditReservation reservation) {
        existing.setOrderId(reservation.orderId());
        existing.setAmountValue(reservation.amount().amount());
        existing.setAmountCurrency(reservation.amount().currency());
        existing.setStatus(reservation.status().name());
        return existing;
    }

    /** Reconstructs a domain {@link CreditReservation} from an entity. */
    public CreditReservation toDomain(CreditReservationEntity entity) {
        Require.notNull(entity, "CreditReservationEntity");

        CreditReservationStatus status = parseStatus(entity.getStatus());
        Money amount = Money.of(entity.getAmountValue(), entity.getAmountCurrency());

        return CreditReservation.reconstruct(
                CreditReservationId.of(entity.getId()),
                entity.getOrderId(),
                amount,
                status,
                entity.getVersion());
    }

    private static CreditReservationStatus parseStatus(String value) {
        try {
            return CreditReservationStatus.valueOf(
                    Require.notBlank(value, "CreditReservationEntity.status"));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Unrecognised CreditReservationStatus in persisted data: '" + value + "'", e);
        }
    }
}
