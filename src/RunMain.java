import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class RunMain {

    public static void main(String[] args) {
        showInfo("Démarrage de la génération de code");
        try {
            Path cwd = Paths.get(".").toAbsolutePath().normalize();

            // Répertoire temporaire
            Path tmpDir = cwd.resolve("tmp");
            deleteDirectory(tmpDir);

            // Copie du template
            Path templateDir = cwd.resolve("Template");
            if (!Files.isDirectory(templateDir)) {
                showError("Dossier 'Template' introuvable");
                System.exit(1);
            }
            copyDirectory(templateDir, tmpDir);

            // Préparation des chemins
            Path entityDir = tmpDir.resolve("src/main/java/app/model/entities");
            Path enumDir = entityDir.resolve("enums");
            Path daoDir = tmpDir.resolve("src/main/java/app/model/dao");
            Path servicesDir = tmpDir.resolve("src/main/java/app/model/services");
            Path interfacesDir = servicesDir.resolve("interfaces");
            Path commandsDir = tmpDir.resolve("src/main/java/app/model/commands");
            Path propertiesFile = tmpDir.resolve("src/main/resources/application.properties");

            Files.createDirectories(entityDir);
            Files.createDirectories(enumDir);
            Files.createDirectories(daoDir);
            Files.createDirectories(servicesDir);
            Files.createDirectories(interfacesDir);
            Files.createDirectories(commandsDir);

            // Lecture de entities.txt
            Path entitiesFile = cwd.resolve("entities.txt");
            if (!Files.exists(entitiesFile)) {
                showError("entities.txt introuvable");
                System.exit(1);
            }
            List<String> lines = Files.readAllLines(entitiesFile, StandardCharsets.UTF_8);

            // Traitement des entités et enums
            for (int i = 0; i < lines.size(); i++) {
                String raw = lines.get(i).trim();
                if (raw.startsWith("c:")) {
                    String name = raw.substring(2).trim();
                    generateEntity(name, lines, i, entityDir);
                    generateDao(name, daoDir);
                    generateServiceInterface(name, interfacesDir);
                    generateServiceImplementation(name, servicesDir);
                    generateCommands(name, commandsDir);
                    i = skipAttributes(lines, i);
                } else if (raw.startsWith("e:")) {
                    String name = raw.substring(2).trim();
                    generateEnum(name, lines, i, enumDir);
                    i = skipAttributes(lines, i);
                } else if (!raw.isEmpty() && !raw.startsWith("-")) {
                    showWarn("Ligne non reconnue dans entities.txt : " + raw);
                }
            }

            // Nettoyage et configuration
            deleteNoneFiles(tmpDir);
            applyProjectSettings(cwd.resolve("projectSettings.txt"), propertiesFile, tmpDir);
            showInfo("Génération terminée");

        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    // 2. Nouvelle signature pour prendre en compte les lignes suivantes :
    private static void generateEnum(String name, List<String> lines, int idxStart, Path enumDir) throws IOException {
        String nl = System.lineSeparator();
        StringBuilder sb = new StringBuilder();

        // Collecte des valeurs de l'enum
        List<String> values = new ArrayList<>();
        int j = idxStart + 1;
        while (j < lines.size() && lines.get(j).trim().startsWith("-")) {
            String rawVal = lines.get(j).trim().replaceFirst("^-+", "").trim();
            // Normalisation : majuscules et underscores
            String enumConst = rawVal
                    .toUpperCase()
                    .replaceAll("\\s+", "_");
            values.add(enumConst);
            j++;
        }

        // Construction du contenu de l'enum
        sb.append("package app.model.entities.enums;").append(nl).append(nl)
                .append("public enum ").append(name).append(" { ");
        if (!values.isEmpty()) {
            sb.append(String.join(", ", values));
        }
        sb.append("; }").append(nl);

        // Écriture du fichier
        Files.write(enumDir.resolve(name + ".java"),
                sb.toString().getBytes(StandardCharsets.UTF_8));
        showInfo("Enum généré: " + name);
    }

    private static void generateEntity(String name, List<String> lines, int idxStart, Path entityDir)
            throws IOException {
        String nl = System.lineSeparator();
        StringBuilder sb = new StringBuilder();
        String tableName = name.toLowerCase();
        List<String> starred = new ArrayList<>();
        List<String> fieldLines = new ArrayList<>();
        Pattern camelCaseBoundary = Pattern.compile("([a-z])([A-Z])");
        Pattern genericPattern = Pattern.compile("<(.+?)>");

        // 1. Collecte des attributs
        int j = idxStart + 1;
        while (j < lines.size() && lines.get(j).trim().startsWith("-")) {
            String attrRaw = lines.get(j).trim().replaceFirst("^-+", "").trim();
            boolean isStar = attrRaw.endsWith("*");
            if (isStar)
                attrRaw = attrRaw.substring(0, attrRaw.length() - 1).trim();

            String[] parts = attrRaw.split("\\s+");
            if (parts.length >= 2) {
                String type = parts[0];
                String var = parts[1];
                String col = camelCaseBoundary.matcher(var)
                        .replaceAll("$1_$2")
                        .toLowerCase();
                String nullableAttr = isStar ? ", nullable=false" : "";

                // 2. Cas relations (List/Set/Map/Collection)
                if (type.startsWith("List") ||
                        type.startsWith("Set") ||
                        type.startsWith("Map") ||
                        type.startsWith("Collection")) {

                    // Récupération du générique
                    Matcher m = genericPattern.matcher(type);
                    String generic = m.find() ? m.group(1).trim() : "";
                    String joinCol = tableName + "_id";
                    String elemCol = camelCaseBoundary.matcher(generic)
                            .replaceAll("$1_$2")
                            .toLowerCase();

                    fieldLines.add("    //TODO: (Code) Ajouter les règles de gestion");

                    // TODO: Faire une vérification de cette partie pour une optimisation
                    // --- Bloc OPTION OneToMany ---
                    fieldLines.add("    // === OPTION OneToMany unidirectionnel ===");
                    fieldLines.add("    // @OneToMany(fetch = FetchType.LAZY)");
                    fieldLines.add("    // @ElementCollection");
                    fieldLines.add("    // @CollectionTable(name = \"" + tableName + "_" + col + "\",");
                    fieldLines.add("    //        joinColumns = @JoinColumn(name = \"" + joinCol + "\"))");
                    fieldLines.add("");

                    // --- Bloc OPTION ManyToMany ---
                    fieldLines.add("    // === OPTION ManyToMany bidirectionnel ===");
                    fieldLines.add("    // @ManyToMany");
                    fieldLines.add("    // @JoinTable(name = \"" + tableName + "_" + col + "\",");
                    fieldLines.add("    //            joinColumns = @JoinColumn(name = \"" + joinCol + "\"),");
                    fieldLines.add("    //            inverseJoinColumns = @JoinColumn(name = \"" + col + "_" + elemCol
                            + "\"))");

                    // Si tu veux un bloc Map<K,V>, rajoute ici un 3e bloc similaire…
                }
                // 3. Cas simple non-relationnel
                else {
                    fieldLines.add("    @Getter @Setter");
                    fieldLines.add("    //TODO: (Code) Ajouter les règles de gestion");
                    if (isStar)
                        fieldLines.add("    @NotNull");
                    fieldLines.add("    @Column(name=\"" + col + "\"" + nullableAttr + ")");
                }

                String variable = attrRaw.replace("*", "");

                fieldLines.add("    private " + variable + ";");
                fieldLines.add("");

                if (isStar)
                    starred.add(var);
            }
            j++;
        }

        // 4. Génération de l’en-tête & écriture du fichier (idem version précédente)
        sb.append("package app.model.entities;").append(nl).append(nl)
                .append("import jakarta.persistence.*;").append(nl)
                .append("import lombok.*;").append(nl)
                .append("import org.springframework.data.jpa.domain.AbstractPersistable;").append(nl)
                .append("import org.springframework.format.annotation.DateTimeFormat;").append(nl).append(nl)
                .append("@Entity").append(nl)
                .append("@Table(name=\"").append(tableName).append("\"");
        if (!starred.isEmpty()) {
            String cols = starred.stream()
                    .map(v -> "\"" + camelCaseBoundary.matcher(v)
                            .replaceAll("$1_$2").toLowerCase() + "\"")
                    .collect(Collectors.joining(", "));
            String ucName = "uk_" + tableName + "_"
                    + starred.stream()
                            .map(v -> camelCaseBoundary.matcher(v)
                                    .replaceAll("$1_$2").toLowerCase())
                            .collect(Collectors.joining("_"));
            sb.append(", uniqueConstraints=@UniqueConstraint(name=\"")
                    .append(ucName)
                    .append("\", columnNames={")
                    .append(cols)
                    .append("})");
        }
        sb.append(")").append(nl)
                .append("@NoArgsConstructor(access=AccessLevel.PROTECTED)").append(nl)
                .append("@ToString(callSuper=false");
        if (!starred.isEmpty()) {
            String ofList = starred.stream()
                    .map(v -> "\"" + camelCaseBoundary.matcher(v)
                            .replaceAll("$1_$2").toLowerCase() + "\"")
                    .collect(Collectors.joining(", "));
            sb.append(", of={").append(ofList).append("}");
        }
        sb.append(")").append(nl)
                .append("@EqualsAndHashCode(callSuper=true");
        if (!starred.isEmpty()) {
            String ofList = starred.stream()
                    .map(v -> "\"" + camelCaseBoundary.matcher(v)
                            .replaceAll("$1_$2").toLowerCase() + "\"")
                    .collect(Collectors.joining(", "));
            sb.append(", of={").append(ofList).append("}");
        }
        sb.append(")").append(nl).append(nl)
                .append("public class ").append(name)
                .append(" extends AbstractPersistable<Long> {").append(nl).append(nl);

        // Insertion des champs
        for (String fld : fieldLines) {
            sb.append(fld).append(nl);
        }
        sb.append("}").append(nl);

        Files.write(entityDir.resolve(name + ".java"),
                sb.toString().getBytes(StandardCharsets.UTF_8));
        showInfo("Entité générée: " + name);
    }

    private static int skipAttributes(List<String> lines, int idxStart) {
        int j = idxStart + 1;
        while (j < lines.size() && lines.get(j).trim().startsWith("-"))
            j++;
        return j - 1;
    }

    private static void generateDao(String name, Path daoDir) throws IOException {
        String src = "package app.model.dao;" + System.lineSeparator()
                + "import app.model.entities." + name + ";" + System.lineSeparator()
                + "import org.springframework.data.jpa.repository.JpaRepository;" + System.lineSeparator()
                + "import org.springframework.stereotype.Repository;" + System.lineSeparator() + System.lineSeparator()
                + "@Repository" + System.lineSeparator()
                + "public interface " + name + "Dao extends JpaRepository<" + name + ",Long> {}";
        Files.write(daoDir.resolve(name + "Dao.java"), src.getBytes(StandardCharsets.UTF_8));
        showInfo("DAO généré: " + name + "Dao");
    }

    private static void generateServiceInterface(String name, Path interfacesDir) throws IOException {
        String src = "package app.model.services.interfaces;" + System.lineSeparator()
                + "import app.model.entities." + name + ";" + System.lineSeparator() + System.lineSeparator()
                + "public interface I" + name + "Service extends IAbstractService<" + name + ",Long> {}";
        Files.write(interfacesDir.resolve("I" + name + "Service.java"), src.getBytes(StandardCharsets.UTF_8));
        showInfo("Service Interface générée: I" + name + "Service");
    }

    private static void generateServiceImplementation(String name, Path servicesDir) throws IOException {
        String src = "package app.model.services;" + System.lineSeparator()
                + "import app.model.dao." + name + "Dao;" + System.lineSeparator()
                + "import app.model.entities." + name + ";" + System.lineSeparator()
                + "import org.springframework.stereotype.Service;" + System.lineSeparator() + System.lineSeparator()
                + "@Service" + System.lineSeparator()
                + "public class " + name + "Service extends AbstractService<" + name + "," + name
                + "Dao,Long> implements I" + name + "Service {" + System.lineSeparator()
                + "    public " + name + "Service(" + name + "Dao dao) { this.repository=dao; }"
                + System.lineSeparator()
                + "}";
        Files.write(servicesDir.resolve(name + "Service.java"), src.getBytes(StandardCharsets.UTF_8));
        showInfo("Service implémentation générée: " + name + "Service");
    }

    private static void generateCommands(String name, Path commandsDir) throws IOException {
        String varName = Character.toLowerCase(name.charAt(0)) + name.substring(1) + "Service";
        String src = "package app.model.commands;" + System.lineSeparator()
                + "import app.model.services.interfaces.I" + name + "Service;" + System.lineSeparator()
                + "import lombok.NonNull;" + System.lineSeparator()
                + "import lombok.RequiredArgsConstructor;" + System.lineSeparator()
                + "import org.jline.terminal.Terminal;" + System.lineSeparator()
                + "import org.springframework.shell.standard.ShellComponent;" + System.lineSeparator()
                + System.lineSeparator()
                + "@ShellComponent" + System.lineSeparator()
                + "@RequiredArgsConstructor" + System.lineSeparator()
                + "public class " + name + "Commands {" + System.lineSeparator()
                + "    @NonNull private final I" + name + "Service " + varName + ";" + System.lineSeparator()
                + "    @NonNull private final Terminal terminal;" + System.lineSeparator()
                + "}";
        Files.write(commandsDir.resolve(name + "Commands.java"), src.getBytes(StandardCharsets.UTF_8));
        showInfo("Commands générés: " + name + "Commands");
    }

    private static void generateEnum(String name, Path enumDir) throws IOException {
        String src = "package app.model.entities.enums;" + System.lineSeparator() + System.lineSeparator()
                + "public enum " + name + " { Ajouter, des, valeurs, ici ,,}" + System.lineSeparator();
        Files.write(enumDir.resolve(name + ".java"), src.getBytes(StandardCharsets.UTF_8));
        showInfo("Enum généré: " + name);
    }

    private static void deleteNoneFiles(Path start) throws IOException {
        Files.walkFileTree(start, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (file.getFileName().toString().equalsIgnoreCase("none"))
                    Files.delete(file);
                return FileVisitResult.CONTINUE;
            }
        });
        showInfo("Fichiers 'none' supprimés");
    }

    private static void applyProjectSettings(Path settingsFile, Path propertiesFile, Path tmpDir) throws IOException {
        if (!Files.exists(settingsFile)) {
            showWarn("projectSettings.txt introuvable");
            return;
        }
        Map<String, String> configs = new HashMap<>();
        for (String line : Files.readAllLines(settingsFile, StandardCharsets.UTF_8)) {
            String[] kv = line.split("=", 2);
            if (kv.length == 2)
                configs.put(kv[0].trim(), kv[1].trim());
            else
                showWarn("Ligne ignorée: " + line);
        }
        String dbKey = "database_name";
        if (configs.containsKey(dbKey) && !configs.get(dbKey).isEmpty())
            changeDatabaseNameConfig(configs.get(dbKey), propertiesFile);
        else if (configs.containsKey(dbKey))
            showWarn("database_name vide, valeur par défaut laissee");
        else
            showWarn("Clé 'database_name' non trouvée");

        String ideKey = "intellij_project_path";
        if (configs.containsKey(ideKey) && !configs.get(ideKey).isEmpty())
            copyDirectory(tmpDir, Paths.get(configs.get(ideKey)));
        else if (configs.containsKey(ideKey))
            showWarn("intellij_project_path vide, le template genere se trouve dans /tmp");
        else
            showWarn("Clé 'intellij_project_path' non trouvée");

        showInfo("projectSettings appliqués");
    }

    private static void changeDatabaseNameConfig(String dbName, Path propFile) {
        try {
            List<String> lines = Files.readAllLines(propFile, StandardCharsets.UTF_8);
            for (int i = 0; i < lines.size(); i++)
                lines.set(i, lines.get(i).replace("DB_NAME_PLACEHOLDER", dbName));
            Files.write(propFile, lines, StandardCharsets.UTF_8);
            showInfo("database_name mis à jour: " + dbName);
        } catch (IOException e) {
            showError("Erreur config: " + e.getMessage());
        }
    }

    private static void copyDirectory(Path source, Path target) throws IOException {
        Files.walkFileTree(source, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                Files.createDirectories(target.resolve(source.relativize(dir)));
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.copy(file, target.resolve(source.relativize(file)), StandardCopyOption.REPLACE_EXISTING);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private static void deleteDirectory(Path start) throws IOException {
        if (!Files.exists(start))
            return;
        Files.walkFileTree(start, new SimpleFileVisitor<Path>() {
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

    private static void showInfo(String msg) {
        System.out.println("[INFO] " + msg);
    }

    private static void showWarn(String msg) {
        System.out.println(
                "******************************************************* WARNING *******************************************************");
        System.out.println("[WARN] " + msg + "\n");
    }

    private static void showError(String msg) {
        System.err.println(
                "******************************************************* ERROR *******************************************************");
        System.err.println("[ERROR] " + msg + "\n");

    }
}
