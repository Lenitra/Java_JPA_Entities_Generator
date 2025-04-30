package app.model.services.interfaces;


import app.model.services.exceptions.ServiceException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface IAbstractService<T, ID> {

    T save(T entity) throws ServiceException;

    Optional<T> findById(ID id) throws ServiceException;

    Page<T> findAll(Pageable pageable) throws ServiceException;

    Iterable<T> findAll() throws ServiceException;

    void deleteById(ID id) throws ServiceException;

    Long count() throws ServiceException;
}
