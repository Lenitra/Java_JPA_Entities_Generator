package model.dao;

import model.dao.exception.DaoException;

import java.util.Optional;
import java.util.OptionalInt;

public interface Dao<T> {

    T save(T entity) throws DaoException;

    Optional<T> findById(Long id) throws DaoException;

    Iterable<T> readAll() throws DaoException;

    void delete(Long id) throws DaoException;
    void delete(T t) throws DaoException;

    boolean exist(Long id) throws DaoException;

    Long count() throws DaoException;
}
