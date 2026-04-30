package io.atalib.domain;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public abstract class AbstractUuidAuditingEntity extends AbstractAuditingBase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    protected UUID id;
}
