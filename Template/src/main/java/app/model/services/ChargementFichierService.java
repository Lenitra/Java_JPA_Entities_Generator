package app.model.services;

import app.model.entities.*;
import app.model.services.exceptions.ServiceException;
import app.model.services.interfaces.IChargementFichierService;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChargementFichierService implements IChargementFichierService {

    // @NonNull
    // GuildeService guildeService;
    // @NonNull
    // PersonnageService personnageService;
    // @NonNull
    // SortService sortService;

    @Override
    public void chargerFichier(Path chemin, String typeFichier) throws ServiceException {

        try {
            List<String> lignes = Files.readAllLines(chemin);
        //     switch (typeFichier.toUpperCase()) {
        //         case "GUILD" -> insererGuild(lignes);
        //         case "SORT"-> insererSort(lignes);
        //         case "PERSONNAGE"-> insererPersonnage(lignes);
        //     }
        } catch (IOException e) {
            throw new ServiceException("Impossible de lire le fichier",e);
        }
    }


    // private void insererSort(List<String> lignes) throws ServiceException {
    //     for(String ligne: lignes) {
    //         String[] ligneSplit = ligne.split(",");
    //         Sort s = EntitiesFactory.fabriquerSort(ligneSplit[1],Integer.parseInt(ligneSplit[2]),Integer.parseInt(ligneSplit[3]));
    //         sortService.save(s);
    //     }
    // }


    // private void insererPersonnage(List<String> lignes) throws ServiceException {
    //     for(String ligne: lignes) {
    //         String[] ligneSplit = ligne.split(",");
    //         Personnage p = EntitiesFactory.fabriquerPersonnage(ligneSplit[1],Integer.parseInt(ligneSplit[2]),Integer.parseInt(ligneSplit[3]),Integer.parseInt(ligneSplit[4]));
    //         personnageService.save(p);
    //     }
    // }


    // private void insererGuild(List<String> lignes) throws ServiceException {
    //     for(String ligne: lignes) {
    //         String[] ligneSplit = ligne.split(",");
    //         Guilde g = EntitiesFactory.fabriquerGuilde(ligneSplit[1], TypeGuilde.valueOf(ligneSplit[2]), LocalDate.parse(ligneSplit[3]));
    //         guildeService.save(g);
    //     }
    // }

}
