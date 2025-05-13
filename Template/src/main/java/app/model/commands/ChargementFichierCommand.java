package app.model.commands;

import app.model.services.ChargementFichierService;
import app.model.commands.commons.*;
import jakarta.validation.constraints.NotBlank;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;

import java.nio.file.Path;
import java.nio.file.Paths;

@ShellComponent
@RequiredArgsConstructor
public class ChargementFichierCommand {

    @NonNull
    private final ShellHelper shellHelper;

    @NonNull
    private final ChargementFichierService chargementFichierService;

    @ShellMethod(value = "permet de charger un fichier", key = "chgt-file")
    public String chargementFichierCommand(
            @ShellOption(value = {"-f","--file"}, help = "chemin complet du fichier source") @NotBlank String file,
            @ShellOption(value = {"-c","--typeClasse"}, help = "Type de la classe pour le fichier concerner") @NotBlank String typeClasse
    ) {
        try {
        Path path = Paths.get(file);

            chargementFichierService.chargerFichier(path,typeClasse);
            return shellHelper.getSuccessMessage("Le fichier a été chargé correctement.");
        } catch (Exception e) {
            shellHelper.printError("Erreur lors du chargement.");
            return shellHelper.getWarningMessage(e.getMessage());
        }
    }

}
