package io.atalib.controller;

import io.atalib.service.GenericService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Contrôleur REST générique exposant les 5 endpoints CRUD standard.
 *
 * <p>Usage : étendre cette classe, annoter avec @RestController + @RequestMapping,
 * et optionnellement @SecuredCrud pour la sécurité déclarative.
 *
 * @param <REQUEST_DTO>  DTO d'entrée
 * @param <RESPONSE_DTO> DTO de sortie
 * @param <ID>           type identifiant
 */
@Slf4j
public abstract class AbstractGenericController<REQUEST_DTO, RESPONSE_DTO, ID> {

    protected final GenericService<REQUEST_DTO, RESPONSE_DTO, ID> service;

    protected AbstractGenericController(GenericService<REQUEST_DTO, RESPONSE_DTO, ID> service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<RESPONSE_DTO> create(@Valid @RequestBody REQUEST_DTO requestDto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(requestDto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<RESPONSE_DTO> update(@PathVariable ID id,
                                               @Valid @RequestBody REQUEST_DTO requestDto) {
        return ResponseEntity.ok(service.update(id, requestDto));
    }

    @GetMapping("/{id}")
    public ResponseEntity<RESPONSE_DTO> getById(@PathVariable ID id) {
        return ResponseEntity.ok(service.get(id));
    }

    @GetMapping
    public ResponseEntity<Page<RESPONSE_DTO>> getAll(Pageable pageable) {
        return ResponseEntity.ok(service.getAll(pageable));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable ID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
