import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RunMain {
    public static void main(String[] args) {
        System.out.println("");
        System.out.println("");
        System.out.println("");
        System.out.println("");
        System.out.println("");
        try {
            // Recupere le dossier courant de maniere valide
            Path cwd = Paths.get("").toAbsolutePath().normalize();

            // 0. Initialize
            Path tmpDir = cwd.resolve("tmp");
            deleteIfExists(tmpDir);

            // 1. Copy template
            Path templateDir = cwd.resolve("Template");
            if (!Files.isDirectory(templateDir)) {
                showError("Aucun dossier 'Template' trouvé");
                System.exit(1);
            }

            copyDirectory(templateDir, tmpDir);

            // Prepare paths
            Path entityDir = tmpDir.resolve("src/main/java/app/model/entities");
            Path daoDir = tmpDir.resolve("src/main/java/app/model/dao");
            Path referenceDir = entityDir.resolve("enums");
            Path servicesDir = tmpDir.resolve("src/main/java/app/model/services");
            Path commandsDir = tmpDir.resolve("src/main/java/app/model/commands");

            Files.createDirectories(entityDir);
            Files.createDirectories(daoDir);
            Files.createDirectories(referenceDir);

            // #region 2. Process entities.txt
            Path entitiesFile = cwd.resolve("entities.txt");
            if (!Files.exists(entitiesFile)) {
                showError("entities.txt introuvable");
                System.exit(1);
            }
            List<String> entityLines = Files.readAllLines(entitiesFile, StandardCharsets.UTF_8);
            if (entityLines.isEmpty()) {
                showError("entities.txt est vide");
                System.exit(1);
            }
            for (String raw : entityLines) {
                String line = raw.trim().replace(" ", "");

                if (line.startsWith("c:")) {
                    String name = line.substring(2);

                    // Entity class
                    String entitySrc = "package app.entities;"
                            + System.lineSeparator() + System.lineSeparator()

                            + "import jakarta.persistence.*;" + System.lineSeparator()
                            + "import lombok.*;" + System.lineSeparator()
                            + "import org.springframework.data.jpa.domain.AbstractPersistable;" + System.lineSeparator()
                            + System.lineSeparator() + System.lineSeparator()

                            + "@Entity" + System.lineSeparator()
                            + "//TODO: Configurer les arguments de la cle primaire" + System.lineSeparator()
                            + "@Table(name = \"" + name.toLowerCase()
                            + "\",uniqueConstraints = @UniqueConstraint(name = \"uk_" + name.toLowerCase()
                            + "__TODOarg1_TODOarg2\", columnNames = {\"TODOarg1\", \"TODOarg2\"}))"
                            + System.lineSeparator()
                            + "@NoArgsConstructor (access = AccessLevel.PROTECTED)" + System.lineSeparator()
                            + "//TODO: Configurer les arguments qui seront affiches sur le ToString"
                            + System.lineSeparator()
                            + "@ToString(callSuper = false, of = {\"TODO\", \"mettre une liste d'arguments\"})"
                            + System.lineSeparator()
                            + "//TODO: Configurer les arguments qui seront marque d'unicite" + System.lineSeparator()
                            + "@EqualsAndHashCode(callSuper = true, of = {\"TODO\", \"mettre une liste d'arguments\"})"
                            + System.lineSeparator()
                            + "@RequiredArgsConstructor (access = AccessLevel.PROTECTED)" + System.lineSeparator()
                            + System.lineSeparator()

                            + "public class " + name + " extends AbstractPersistable<Long> {"
                            + System.lineSeparator() + System.lineSeparator()
                            + "}" + System.lineSeparator();

                    Files.write(entityDir.resolve(name + ".java"), entitySrc.getBytes(StandardCharsets.UTF_8));

                    // DAO de chaque entite
                    String implSrc = "package app.model.dao;\n" +
                            "\n" +
                            "import app.model.entities." + name + ";\n" +
                            "import org.springframework.data.jpa.repository.JpaRepository;\n" +
                            "import org.springframework.stereotype.Repository;\n" +
                            "\n" +
                            "@Repository\n" +
                            "public interface " + name + "Dao extends JpaRepository<" + name + ", Long> {\n" +
                            "}\n";

                    Files.write(daoDir.resolve(name + "Dao.java"), implSrc.getBytes(StandardCharsets.UTF_8));

                    // Service interface
                    String interfaceService = "package app.model.services;" + System.lineSeparator()
                            + System.lineSeparator()
                            + "import app.model.entities." + name + ";" + System.lineSeparator()
                            + System.lineSeparator()
                            + "public interface I" + name + "Service extends IAbstractService<" + name + ", Long> {"
                            + System.lineSeparator()
                            + System.lineSeparator()
                            + "}" + System.lineSeparator();
                    Files.write(servicesDir.resolve("Interfaces/I" + name + "Service.java"),
                            interfaceService.getBytes(StandardCharsets.UTF_8));

                    // Service implémentation
                    String serviceImplement = "package app.model.services;\n" +
                            "\n" +
                            "import app.model.dao." + name + "Dao;\n" +
                            "import app.model.entities." + name + ";\n" +
                            "import org.springframework.stereotype.Service;\n" +
                            "\n" +
                            "@Service\n" +
                            "public class " + name + "Service extends AbstractService<" + name + ", " + name
                            + "Dao, Long> implements I" + name + "Service {\n" +
                            "\n" +
                            "    public " + name + "Service(" + name + "Dao dao) {\n" +
                            "        this.repository = dao;\n" +
                            "    }\n" +
                            "\n" +
                            "}\n";

                    Files.write(servicesDir.resolve("" + name + "Service.java"),
                            serviceImplement.getBytes(StandardCharsets.UTF_8));

                    String commands = "package app.model.commands;\n\n" +
                            "import app.model.services.I" + name + "Service;\n" +
                            "import lombok.NonNull;\n" +
                            "import lombok.RequiredArgsConstructor;\n" +
                            "import org.jline.terminal.Terminal;\n" +
                            "import org.springframework.shell.standard.ShellComponent;\n\n" +
                            "@ShellComponent\n" +
                            "@RequiredArgsConstructor\n" +
                            "public class " + name + "Commands {\n\n" +
                            "    @NonNull\n" +
                            "    private final I" + name + "Service " + name.toLowerCase() + "Service;\n\n" +
                            "    @NonNull\n" +
                            "    private Terminal terminal;\n\n" +
                            "}\n";

                    Files.write(commandsDir.resolve(name + "Commands.java"),
                            commands.getBytes(StandardCharsets.UTF_8));
                }

                else if (line.startsWith("e:")) {
                    String name = line.substring(2);
                    String enumSrc = "package entities.enums;" + System.lineSeparator()
                            + System.lineSeparator()
                            + "public enum " + name + " {" + System.lineSeparator()
                            + "    // TODO: add values" + System.lineSeparator()
                            + "}" + System.lineSeparator();
                    Files.write(referenceDir.resolve(name + ".java"), enumSrc.getBytes(StandardCharsets.UTF_8));
                }
            }

            // #endregion

            // Suppression des fichiers "none"
            Files.walkFileTree(tmpDir, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    if (file.getFileName().toString().equals("none")) {
                        Files.delete(file);
                    }
                    return FileVisitResult.CONTINUE;
                }
            });

            // Appliquer les configurations au projet
            Path projectConfig = cwd.resolve("projectSettings.txt");
            Map<String, String> configs = new HashMap<>();
            for (String line : Files.readAllLines(projectConfig, StandardCharsets.UTF_8)) {
                String[] keyValue = line.split("=", 2);
                if (keyValue.length == 2) {
                    configs.put(keyValue[0].trim(), keyValue[1].trim());
                } else {
                    showWarn("Ligne ignoree dans projectSettings.txt : " + line);
                }
            }

            // Changement de la valeur
            if (configs.containsKey("database_name")) {
                String value = configs.get("database_name");
                if (value.isEmpty()) {
                    showWarn("database_name est vide. Valeur par defaut laissee.");
                } else {
                    changeDatabaseNameConfig(value);
                }
            } else {
                showWarn("La clef 'database_name' n'a pas ete trouvee dans projectSettings.txt.");
            }

            // Copie du dossier temporaire vers le chemin du projet IntelliJ si la
            // configuration est présente
            if (configs.containsKey("inteliJ_project_path")) {
                String value = configs.get("inteliJ_project_path");
                if (value.isEmpty()) {
                    showWarn("database_name est vide. Le dossier genere 'tmp' ne sera pas deplace.");
                } else {
                    copyDirectory(tmpDir, Paths.get(value));
                }
            } else {
                showWarn("La clef 'inteliJ_project_path' n'a pas ete trouvee dans projectSettings.txt.");
            }

            showInfos("-----------");
            showInfos(". TERMINE .");
            showInfos("-----------");


        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
        System.out.println("");
        System.out.println("");
        System.out.println("");
        System.out.println("");
        System.out.println("");
    }

    private static void copyDirectory(Path source, Path target) throws IOException {
        Files.walkFileTree(source, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                Path rel = source.relativize(dir);
                Files.createDirectories(target.resolve(rel));
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Path rel = source.relativize(file);
                Files.copy(file, target.resolve(rel), StandardCopyOption.REPLACE_EXISTING);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private static void deleteIfExists(Path path) throws IOException {
        if (!Files.exists(path))
            return;
        Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private static void changeDatabaseNameConfig(String databaseName) {
        // TODO
    }

    public static void showInfos(String message) {
        String prefix = "[INFO]";
        System.out.println(prefix + message);
    }

    public static void showWarn(String message) {
        String prefix = "[WARN]";
        System.out.println(prefix + message);
    }

    public static void showError(String message) {
        String prefix = "[ERRO]";
        System.out.println(prefix + message);
    }

}
