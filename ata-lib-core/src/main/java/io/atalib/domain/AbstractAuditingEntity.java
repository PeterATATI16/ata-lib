package io.atalib.domain;

import io.atalib.util.AuditUtils;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class AbstractAuditingEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    protected Long id;

    @CreatedDate
    @Column(updatable = false)
    protected LocalDateTime createdAt;

    @LastModifiedDate
    protected LocalDateTime updatedAt;

    @Column(updatable = false)
    protected String createdBy;

    protected String updatedBy;

    protected String deletedBy;

    @Column(nullable = false)
    protected Boolean deleted = false;

    @PrePersist
    protected void onPrePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.deleted == null) this.deleted = false;
        this.createdBy = AuditUtils.getCurrentUsername();
        this.updatedBy = AuditUtils.getCurrentUsername();
        beforePersist();
    }

    @PreUpdate
    protected void onPreUpdate() {
        this.updatedAt = LocalDateTime.now();
        this.updatedBy = AuditUtils.getCurrentUsername();
        beforePersist();
    }

    @PreRemove
    protected void onPreRemove() {
        this.deleted = true;
        this.deletedBy = AuditUtils.getCurrentUsername();
    }

    public void softDelete() {
        this.deleted = Boolean.TRUE;
        this.deletedBy = AuditUtils.getCurrentUsername();
        this.updatedAt = LocalDateTime.now();
    }

    /** Override pour exécuter de la logique custom avant chaque persist/update. */
    protected void beforePersist() {
    }
}
