package io.atalib.service;

import io.atalib.domain.AbstractAuditingBase;
import io.atalib.exception.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AbstractGenericServiceTest {

    // -------------------------------------------------------------------------
    // Test fixtures
    // -------------------------------------------------------------------------

    static class SimpleEntity {
        Long id;
        String name;
    }

    static class AuditEntity extends AbstractAuditingBase {
        Long id;
        String name;
    }

    static class Req {
        String name;
    }

    static class Res {
        String name;
    }

    /** Concrete service that records lifecycle hook invocations. */
    static class TrackingService extends AbstractGenericService<SimpleEntity, Req, Res, Long> {
        final List<String> hooks = new ArrayList<>();

        TrackingService(JpaRepository<SimpleEntity, Long> repo) {
            super(repo,
                    req -> { var e = new SimpleEntity(); e.name = req.name; return e; },
                    e -> { var r = new Res(); r.name = e.name; return r; },
                    (e, req) -> e.name = req.name);
        }

        @Override protected void beforeCreate(Req r)                         { hooks.add("beforeCreate"); }
        @Override protected void afterMapping(SimpleEntity e, Req r)         { hooks.add("afterMapping"); }
        @Override protected void afterCreate(SimpleEntity e, Req r)          { hooks.add("afterCreate"); }
        @Override protected void beforeUpdate(Long id, Req r)                { hooks.add("beforeUpdate"); }
        @Override protected void afterUpdateMapping(SimpleEntity e, Req r)   { hooks.add("afterUpdateMapping"); }
        @Override protected void afterUpdate(SimpleEntity e, Req r)          { hooks.add("afterUpdate"); }
        @Override protected void beforeDelete(Long id)                       { hooks.add("beforeDelete"); }
        @Override protected void afterDelete(Long id)                        { hooks.add("afterDelete"); }
    }

    @SuppressWarnings("unchecked")
    private final JpaRepository<SimpleEntity, Long> repo = mock(JpaRepository.class);
    private TrackingService service;

    @BeforeEach
    void setup() {
        service = new TrackingService(repo);
    }

    // -------------------------------------------------------------------------
    // Create lifecycle
    // -------------------------------------------------------------------------

    @Test
    void create_invokesHooksInOrder() {
        when(repo.save(any())).thenReturn(new SimpleEntity());

        service.create(new Req());

        assertThat(service.hooks).containsExactly("beforeCreate", "afterMapping", "afterCreate");
    }

    @Test
    void create_persistsEntityAndReturnsDto() {
        SimpleEntity saved = new SimpleEntity();
        saved.name = "Alice";
        when(repo.save(any())).thenReturn(saved);

        Res result = service.create(new Req());

        assertThat(result.name).isEqualTo("Alice");
        verify(repo).save(any(SimpleEntity.class));
    }

    // -------------------------------------------------------------------------
    // Update lifecycle
    // -------------------------------------------------------------------------

    @Test
    void update_invokesHooksInOrder() {
        SimpleEntity existing = new SimpleEntity();
        when(repo.findById(1L)).thenReturn(Optional.of(existing));
        when(repo.save(any())).thenReturn(existing);

        service.update(1L, new Req());

        assertThat(service.hooks).containsExactly("beforeUpdate", "afterUpdateMapping", "afterUpdate");
    }

    @Test
    void update_throwsWhenEntityNotFound() {
        when(repo.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(99L, new Req()))
                .isInstanceOf(EntityNotFoundException.class);
    }

    // -------------------------------------------------------------------------
    // Delete — hard vs soft
    // -------------------------------------------------------------------------

    @Test
    void delete_hardDeletesNonAuditingEntity() {
        SimpleEntity entity = new SimpleEntity();
        when(repo.findById(1L)).thenReturn(Optional.of(entity));

        service.delete(1L);

        verify(repo).delete(entity);
        verify(repo, never()).save(any());
        assertThat(service.hooks).containsExactly("beforeDelete", "afterDelete");
    }

    @Test
    @SuppressWarnings("unchecked")
    void delete_softDeletesAuditingEntity() {
        JpaRepository<AuditEntity, Long> auditRepo = mock(JpaRepository.class);
        var auditService = new AbstractGenericService<AuditEntity, Req, Res, Long>(
                auditRepo,
                req -> new AuditEntity(),
                e -> new Res(),
                (e, req) -> {}) {};

        AuditEntity entity = new AuditEntity();
        when(auditRepo.findById(1L)).thenReturn(Optional.of(entity));
        when(auditRepo.save(any())).thenReturn(entity);

        auditService.delete(1L);

        assertThat(entity.getDeleted()).isTrue();
        verify(auditRepo).save(entity);
        verify(auditRepo, never()).delete(any());
    }

    // -------------------------------------------------------------------------
    // Get
    // -------------------------------------------------------------------------

    @Test
    void get_throwsEntityNotFoundExceptionWhenMissing() {
        when(repo.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(99L))
                .isInstanceOf(EntityNotFoundException.class);
    }

    // -------------------------------------------------------------------------
    // getAllWithoutPagination — soft-delete filtering
    // -------------------------------------------------------------------------

    @Test
    @SuppressWarnings("unchecked")
    void getAllWithoutPagination_excludesDeletedEntities() {
        JpaRepository<AuditEntity, Long> auditRepo = mock(JpaRepository.class);
        var auditService = new AbstractGenericService<AuditEntity, Req, Res, Long>(
                auditRepo,
                req -> new AuditEntity(),
                e -> new Res(),
                (e, req) -> {}) {};

        AuditEntity active = new AuditEntity();
        AuditEntity deleted = new AuditEntity();
        deleted.setDeleted(true);

        when(auditRepo.findAll()).thenReturn(List.of(active, deleted));

        List<Res> result = auditService.getAllWithoutPagination();

        assertThat(result).hasSize(1);
    }

    @Test
    void getAllWithoutPagination_returnsAllNonAuditingEntities() {
        SimpleEntity e1 = new SimpleEntity();
        SimpleEntity e2 = new SimpleEntity();
        when(repo.findAll()).thenReturn(List.of(e1, e2));

        List<Res> result = service.getAllWithoutPagination();

        assertThat(result).hasSize(2);
    }

    // -------------------------------------------------------------------------
    // getAll (paginated)
    // -------------------------------------------------------------------------

    @Test
    void getAll_returnsMappedPage() {
        SimpleEntity e = new SimpleEntity();
        e.name = "Bob";
        when(repo.findAll(any(org.springframework.data.domain.PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(e)));

        var page = service.getAll(Pageable.ofSize(10));

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).name).isEqualTo("Bob");
    }
}
