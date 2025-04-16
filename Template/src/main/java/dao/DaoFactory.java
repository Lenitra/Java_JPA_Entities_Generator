package model.dao.Impl;

import lombok.*;
import model.dao.BarDao;
import model.dao.ClientDao;
import model.dao.CommandeDao;
import model.dao.ConsommationDao;
import model.dao.Impl.bdd.CommandeDaoImpl;
import model.dao.Impl.bdd.ConsommationDaoImpl;
import model.dao.Impl.bdd.BarDaoImpl;
import model.dao.Impl.bdd.ClientDaoImpl;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class DaoFactory {

    private static ClientDao instanceClientDao;
    private static CommandeDao instanceCommandeDao;
    private static ConsommationDao instanceConsommationDao;
    private static BarDao instanceBarDao;


    public static ClientDao fabriquerClientDao(){
        if(instanceClientDao == null){
            instanceClientDao = new ClientDaoImpl();
        }
        return instanceClientDao;
    }

    public static BarDao fabriquerBarDao(){
        if(instanceBarDao == null){
            instanceBarDao = new BarDaoImpl();
        }
        return instanceBarDao;
    }

    public static CommandeDao fabriquerCommandeDao(){
        if(instanceCommandeDao == null){
            instanceCommandeDao = new CommandeDaoImpl();
        }
        return instanceCommandeDao;
    }

    public static ConsommationDao fabriquerConsommationDao(){
        if(instanceConsommationDao == null){
            instanceConsommationDao = new ConsommationDaoImpl();
        }
        return instanceConsommationDao;
    }
}
