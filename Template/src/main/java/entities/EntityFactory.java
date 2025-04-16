package model.entities;

import jakarta.validation.*;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import model.entities.common.ValidException;
import model.entities.common.ValidUtil;
import model.entities.exceptions.ClientException;
import model.entities.references.TypeBoisson;
import model.entities.references.Verre;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class EntityFactory {

    // public static Commande fabriquerCommande(LocalDateTime dateCmd,Client client) throws ValidException {
    //     Commande cmd = new Commande(dateCmd,client);
    //     return ValidUtil.isValide(cmd);
    // }

}
