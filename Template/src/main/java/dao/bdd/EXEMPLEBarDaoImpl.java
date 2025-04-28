package model.dao.Impl.bdd;

import model.dao.BarDao;
import model.dao.Impl.bdd.AbstractDaoImpl;
import model.entities.Bar;

public class EXEMPLEBarDaoImpl  extends AbstractDaoImpl<Bar> implements BarDao { // Class declaration extending AbstractDaoImpl and implementing BarDao

    @Override
    public List<User> findByName(String name) { // Method to find users by name
        return em.createQuery( // Create a query
                    "SELECT u FROM User u WHERE u.name = :name", User.class) // JPQL query to select users by name
                 .setParameter("name", name) // Set the parameter for the query
                 .getResultList(); // Execute the query and return the result list
    }

    public Bar findOrCreate(Bar bar) { // Method to find or create a Bar entity
        String jpql = "SELECT b FROM Bar b WHERE b.name = :name"; // JPQL query to find a Bar by name
        
        Bar existingBar = em.createQuery(jpql, Bar.class) // Execute the query
                .setParameter("name", bar.getName()) // Set the parameter for the query
                .getSingleResult(); // Get a single result

        if (existingBar != null) { // Check if the Bar exists
            return existingBar; // Return the existing Bar
        } else { // If the Bar does not exist
            return save(bar); // Save and return the new Bar
        }
    }

    public List<Object[]> findCustomDataWithNativeQuery() { // Method to execute a native SQL query
        String sql = "SELECT column1, column2 FROM table_name WHERE condition = ?"; // Native SQL query
        return em.createNativeQuery(sql) // Create a native query
                 .setParameter(1, "value") // Set the parameter for the query 
                 .getResultList(); // Execute the query and return the result list
    }
    
}
