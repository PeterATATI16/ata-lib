package io.atalib.exception;

public class EntityNotFoundException extends RuntimeException {

    public EntityNotFoundException(String message) {
        super(message);
    }

    public static EntityNotFoundException forId(Object id) {
        return new EntityNotFoundException("Entity not found with id: " + id);
    }
}
