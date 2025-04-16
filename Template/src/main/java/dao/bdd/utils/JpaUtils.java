package model.dao.Impl.bdd.utils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class JpaUtils {

    private static EntityManager em;

    public static EntityManager getEm(String unitName) {
        if (em == null || !em.isOpen()) {
            em = Persistence.createEntityManagerFactory(unitName).createEntityManager();
        }
        return em;
    }
}
