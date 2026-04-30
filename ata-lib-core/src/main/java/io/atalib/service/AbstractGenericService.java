package io.atalib.service;

import io.atalib.domain.AbstractAuditingEntity;
import io.atalib.exception.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * Implémentation générique du CRUD avec hooks lifecycle.
 *
 * <p>Usage : étendre cette classe et fournir le repository + les fonctions mapper via le constructeur.
 * Surcharger les hooks (beforeCreate, afterMapping, etc.) pour ajouter de la logique custom.
 *
 * @param <ENTITY>       entité JPA
 * @param <REQUEST_DTO>  DTO d'entrée
 * @param <RESPONSE_DTO> DTO de sortie
 * @param <ID>           type identifiant
 */
@Slf4j
public abstract class AbstractGenericService<ENTITY, REQUEST_DTO, RESPONSE_DTO, ID>
        implements GenericService<REQUEST_DTO, RESPONSE_DTO, ID> {

    protected final JpaRepository<ENTITY, ID> repository;
    protected final Function<REQUEST_DTO, ENTITY> dtoToEntityMapper;
    protected final Function<ENTITY, RESPONSE_DTO> entityToDtoMapper;
    protected final BiConsumer<ENTITY, REQUEST_DTO> updateEntityMapper;

    protected AbstractGenericService(
            JpaRepository<ENTITY, ID> repository,
            Function<REQUEST_DTO, ENTITY> dtoToEntityMapper,
            Function<ENTITY, RESPONSE_DTO> entityToDtoMapper,
            BiConsumer<ENTITY, REQUEST_DTO> updateEntityMapper) {
        this.repository = repository;
        this.dtoToEntityMapper = dtoToEntityMapper;
        this.entityToDtoMapper = entityToDtoMapper;
        this.updateEntityMapper = updateEntityMapper;
    }

    // -------------------------------------------------------------------------
    // CRUD
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public RESPONSE_DTO create(REQUEST_DTO requestDto) {
        beforeCreate(requestDto);
        ENTITY entity = dtoToEntityMapper.apply(requestDto);
        afterMapping(entity, requestDto);
        ENTITY saved = repository.save(entity);
        afterCreate(saved, requestDto);
        return entityToDtoMapper.apply(saved);
    }

    @Override
    @Transactional
    public RESPONSE_DTO update(ID id, REQUEST_DTO requestDto) {
        beforeUpdate(id, requestDto);
        ENTITY entity = repository.findById(id)
                .orElseThrow(() -> EntityNotFoundException.forId(id));
        updateEntityMapper.accept(entity, requestDto);
        afterUpdateMapping(entity, requestDto);
        ENTITY updated = repository.save(entity);
        afterUpdate(updated, requestDto);
        return entityToDtoMapper.apply(updated);
    }

    @Override
    @Transactional(readOnly = true)
    public RESPONSE_DTO get(ID id) {
        ENTITY entity = repository.findById(id)
                .orElseThrow(() -> EntityNotFoundException.forId(id));
        return entityToDtoMapper.apply(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RESPONSE_DTO> getAll(Pageable pageable) {
        return fetchEntities(pageable).map(entityToDtoMapper);
    }

    @Override
    @Transactional
    public void delete(ID id) {
        beforeDelete(id);
        ENTITY entity = repository.findById(id)
                .orElseThrow(() -> EntityNotFoundException.forId(id));
        if (entity instanceof AbstractAuditingEntity baseEntity) {
            baseEntity.softDelete();
            repository.save(entity);
        } else {
            repository.delete(entity);
        }
        afterDelete(id);
    }

    // -------------------------------------------------------------------------
    // Hook pour personnaliser la récupération (filtrage par rôle, tenant, etc.)
    // -------------------------------------------------------------------------

    /**
     * Surcharger pour filtrer les entités retournées par getAll (ex: par utilisateur, rôle, tenant…).
     * Par défaut, retourne toutes les entités triées par updatedAt DESC.
     */
    protected Page<ENTITY> fetchEntities(Pageable pageable) {
        Sort sort = Sort.by(Sort.Direction.DESC, "updatedAt");
        PageRequest pageRequest = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);
        return repository.findAllByDeletedFalse(pageRequest);
    }

    // -------------------------------------------------------------------------
    // Hooks lifecycle — surcharger dans les sous-classes au besoin
    // -------------------------------------------------------------------------

    /** Appelé avant le mapping DTO → Entity dans create(). */
    protected void beforeCreate(REQUEST_DTO requestDto) {
    }

    /** Appelé après le mapping DTO → Entity, avant le save dans create(). */
    protected void afterMapping(ENTITY entity, REQUEST_DTO requestDto) {
    }

    /** Appelé après le save dans create(). */
    protected void afterCreate(ENTITY entity, REQUEST_DTO requestDto) {
    }

    /** Appelé avant le mapping DTO → Entity dans update(). */
    protected void beforeUpdate(ID id, REQUEST_DTO requestDto) {
    }

    /** Appelé après le mapping partiel, avant le save dans update(). */
    protected void afterUpdateMapping(ENTITY entity, REQUEST_DTO requestDto) {
    }

    /** Appelé après le save dans update(). */
    protected void afterUpdate(ENTITY entity, REQUEST_DTO requestDto) {
    }

    /** Appelé avant la suppression dans delete(). */
    protected void beforeDelete(ID id) {
    }

    /** Appelé après la suppression dans delete(). */
    protected void afterDelete(ID id) {
    }
}
