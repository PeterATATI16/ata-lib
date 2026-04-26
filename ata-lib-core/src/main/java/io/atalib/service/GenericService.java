package io.atalib.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Contrat CRUD générique.
 *
 * @param <REQUEST_DTO>  DTO d'entrée (création / mise à jour)
 * @param <RESPONSE_DTO> DTO de sortie
 * @param <ID>           type de l'identifiant
 */
public interface GenericService<REQUEST_DTO, RESPONSE_DTO, ID> {

    RESPONSE_DTO create(REQUEST_DTO requestDto);

    RESPONSE_DTO update(ID id, REQUEST_DTO requestDto);

    RESPONSE_DTO get(ID id);

    Page<RESPONSE_DTO> getAll(Pageable pageable);

    void delete(ID id);

    List<RESPONSE_DTO> getAllWithoutPagination();
}
