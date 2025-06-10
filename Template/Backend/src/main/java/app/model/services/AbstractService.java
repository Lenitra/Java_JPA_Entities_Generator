package app.model.services;

import app.model.services.exceptions.ServiceException;
import app.model.services.interfaces.IAbstractService;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public abstract class AbstractService<T, R extends JpaRepository<T, ID>, ID> implements IAbstractService<T, ID> {

    protected R repository;

    @Override
    public T save(T entity) throws ServiceException {
        try {
            return repository.save(entity);
        } catch (Exception e) {
            throw new ServiceException(e.getMessage(), e);
        }
    }

    @Override
    public Optional<T> findById(ID id) throws ServiceException {
        try {
            return repository.findById(id);
        } catch (Exception e) {
            throw new ServiceException(e.getMessage(), e);
        }
    }

    @Override
    public Page<T> findAll(Pageable pageable) throws ServiceException {
        try {
            return repository.findAll(pageable);
        } catch (Exception e) {
            throw new ServiceException(e.getMessage(), e);
        }
    }

    @Override
    public Iterable<T> findAll() throws ServiceException {
        try {
            return repository.findAll();
        } catch (Exception e) {
            throw new ServiceException(e.getMessage(), e);
        }
    }

    @Override
    public void deleteById(ID id) throws ServiceException {
        try {
            repository.deleteById(id);
        } catch (Exception e) {
            throw new ServiceException(e.getMessage(), e);
        }
    }

    @Override
    public void delete(T entity) throws ServiceException {
        try {
            repository.delete(entity);
        } catch (Exception e) {
            throw new ServiceException(e.getMessage(), e);
        }
    }

    @Override
    public Long count() throws ServiceException {
        try {
            return repository.count();
        } catch (Exception e) {
            throw new ServiceException(e.getMessage(), e);
        }
    }


}
