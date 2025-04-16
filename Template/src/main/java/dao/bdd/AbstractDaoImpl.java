package model.dao.Impl.bdd;

import model.dao.Dao;
import model.dao.Impl.bdd.utils.JpaUtils;
import model.dao.exception.DaoException;
import model.entities.AbstractEntity;

import jakarta.persistence.*;
import java.util.Optional;

public abstract class AbstractDaoImpl<T extends AbstractEntity> implements Dao<T> {

    private Class<T> entityClass;
    protected EntityManager em = JpaUtils.getEm("bar");

    @Override
    public T save(T entity) throws DaoException {
        EntityTransaction transaction = null;
        try{
            transaction = em.getTransaction();
            transaction.begin();
            T retour;
            if(this.em.contains(entity)){
                retour = this.em.merge(entity);
            }else{
               this.em.persist(entity);
               retour = entity;
            }
            transaction.commit();
            return retour;
        }catch (Exception e) {
            transaction.rollback();
            throw new DaoException(e);
        }
    }

    @Override
    public Optional<T> findById(Long id) throws DaoException {
        try{
            return Optional.ofNullable(this.em.find(this.entityClass,id));
        }catch(Exception e){
            throw new DaoException(e);
        }

    }

    @Override
    public Iterable<T> readAll() throws DaoException {
        try {
            return this.em.createQuery("select e from "+this.entityClass.getSimpleName()+" e", this.entityClass).getResultList();
        } catch (Exception e) {
            throw new DaoException(e);
        }
    }

    @Override
    public void delete(Long id) throws DaoException {

        EntityTransaction transaction = null;
        try{
            transaction = em.getTransaction();
            transaction.begin();
            this.em.remove(this.findById(id));
            transaction.commit();
        } catch (Exception e) {
            transaction.rollback();
            throw new DaoException(e);
        }
    }

    @Override
    public void delete(T t) throws DaoException {

        EntityTransaction transaction = null;
        try{
            transaction = em.getTransaction();
            transaction.begin();
            this.em.remove(t);
            transaction.commit();
        } catch (Exception e) {
            transaction.rollback();
            throw new DaoException(e);
        }
    }

    @Override
    public boolean exist(Long id) throws DaoException {
        try {
            return this.findById(id).isPresent();
        } catch (Exception e) {
            throw new DaoException(e);
        }
    }

    @Override
    public Long count() throws DaoException {
        try {
        return this.em.createQuery("select count(e) from "+this.entityClass.getSimpleName()+" e", Long.class).getSingleResult();
        } catch (Exception e) {
            throw new DaoException(e);
        }
    }
}
