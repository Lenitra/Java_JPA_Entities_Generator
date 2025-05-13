package app.model.services.interfaces;


import app.model.services.exceptions.ServiceException;
import java.nio.file.Path;

public interface ChargementFichierService {

    void chargerFichier(Path chemin, String typeFichier) throws ServiceException;
}
