import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.stream.Collectors;

public class RunMain {
    public static void main(String[] args) {
        try {
            // Récupère le dossier courant de manière valide
            Path cwd = Paths.get("").toAbsolutePath().normalize();

            // 0. Initialize
            Path tmpDir = cwd.resolve("tmp");
            deleteIfExists(tmpDir);

            // 1. Copy template
            Path templateDir = cwd.resolve("Template");
            if (!Files.isDirectory(templateDir)) {
                System.err.println("ERREUR : Template directory not found.");
                System.exit(1);
            }
            copyDirectory(templateDir, tmpDir);

            // Prepare paths
            Path entityDir = tmpDir.resolve("src/main/java/entities");
            Path daoDir = tmpDir.resolve("src/main/java/dao");
            Path metierDirImpl = tmpDir.resolve("src/main/java/metier/metierImpl");
            Path referenceDir = entityDir.resolve("enums");

            Files.createDirectories(entityDir);
            Files.createDirectories(daoDir);
            Files.createDirectories(referenceDir);

            // 2. Process entities.txt
            Path entitiesFile = cwd.resolve("entities.txt");
            if (!Files.exists(entitiesFile)) {
                System.err.println("ERREUR : entities.txt not found.");
                System.exit(1);
            }
            List<String> entityLines = Files.readAllLines(entitiesFile, StandardCharsets.UTF_8);
            if (entityLines.isEmpty()) {
                System.err.println("ERREUR : entities.txt is empty.");
                System.exit(1);
            }
            for (String raw : entityLines) {
                String line = raw.trim().replace(" ", "");

                if (line.startsWith("c:")) {
                    String name = line.substring(2);
                    // Entity class
                    String entitySrc = "package entities;"
                            + System.lineSeparator() + System.lineSeparator()


                            + "import jakarta.persistence.*;" + System.lineSeparator()
                            + "import lombok.*;" + System.lineSeparator()
                            + "import org.springframework.data.jpa.domain.AbstractPersistable;" + System.lineSeparator()
                            + System.lineSeparator() + System.lineSeparator()


                            + "@Entity" + System.lineSeparator()
                            + "//TODO: Configurer les arguments de la clé primaire" + System.lineSeparator()
                            + "@Table(name = \"" + name.toLowerCase()
                            + "\",uniqueConstraints = @UniqueConstraint(name = \"uk_" + name.toLowerCase()
                            + "__TODOarg1_TODOarg2\", columnNames = {\"TODOarg1\", \"TODOarg2\"}))"
                            + System.lineSeparator()
                            + "@NoArgsConstructor (access = AccessLevel.PROTECTED)" + System.lineSeparator()
                            + "//TODO: Configurer les arguments qui seront affichés sur le ToString"
                            + System.lineSeparator()
                            + "@ToString(callSuper = false, of = {\"TODO\", \"mettre une liste d'arguments\"})"
                            + System.lineSeparator()
                            + "//TODO: Configurer les arguments qui seront marque d'unicité" + System.lineSeparator()
                            + "@EqualsAndHashCode(callSuper = true, of = {\"TODO\", \"mettre une liste d'arguments\"})"
                            + System.lineSeparator()
                            + "@RequiredArgsConstructor (access = AccessLevel.PROTECTED)" + System.lineSeparator()
                            + System.lineSeparator()

                            + "public class " + name + " extends AbstractPersistable<Long> {"
                            + System.lineSeparator() + System.lineSeparator()
                            + "}" + System.lineSeparator();
                            
                    Files.write(entityDir.resolve(name + ".java"), entitySrc.getBytes(StandardCharsets.UTF_8));

                    // DAO de chaque entité
                    String implSrc = "package dao;" + System.lineSeparator()
                            + System.lineSeparator()
                            + "import entities." + name + ";" + System.lineSeparator()
                            + System.lineSeparator()
                            + System.lineSeparator()
                            + "@Repository" + System.lineSeparator()
                            + "public interface " + name + "Dao extends JpaRepository<" + name + ", Long> {"
                            + System.lineSeparator()
                            + "}" + System.lineSeparator();
                    Files.write(daoDir.resolve(name + "Dao.java"), implSrc.getBytes(StandardCharsets.UTF_8));
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

            // 2.4 Création de l'entity factory
            String entityFactorySrc = "package entities;" + System.lineSeparator()
                    + System.lineSeparator()
                    + "import jakarta.validation.*;" + System.lineSeparator()
                    + "import lombok.AccessLevel;" + System.lineSeparator()
                    + "import lombok.NoArgsConstructor;" + System.lineSeparator()
                    + System.lineSeparator()
                    + "import java.time.LocalDate;" + System.lineSeparator()
                    + "import java.time.LocalDateTime;" + System.lineSeparator()
                    + "import java.util.Set;" + System.lineSeparator()
                    + System.lineSeparator()
                    + "@NoArgsConstructor(access = AccessLevel.PRIVATE)" + System.lineSeparator()
                    + "public final class EntityFactory {" + System.lineSeparator()
                    + System.lineSeparator();
            for (String raw : entityLines) {
                String line = raw.trim().replace(" ", "");
                if (line.startsWith("c:")) {
                    String name = line.substring(2);
                    entityFactorySrc += "    public static " + name + " fabriquer" + name + "() {"
                            + System.lineSeparator()
                            + "        private " + name + " " + name.toLowerCase() + " = new " + name + "();"
                            + System.lineSeparator()
                            + "        // TODO: Appeller les setters ici" + System.lineSeparator()
                            + "        return new " + name.toLowerCase() + ";" + System.lineSeparator()
                            + "    }" + System.lineSeparator();
                }
            }
            entityFactorySrc += "}" + System.lineSeparator();

            Files.write(entityDir.resolve("EntityFactory.java"), entityFactorySrc.getBytes(StandardCharsets.UTF_8));

            // 2.5 Création de la classe MetierImpl.java qui implémente TODO et à en
            // attributs les DAOs
            String metierSrc = "package metier.metierImpl;" + System.lineSeparator()
                    + System.lineSeparator()
                    + "import dao.*;" + System.lineSeparator()
                    + System.lineSeparator()
                    + "// TODO: Gérer l'implementation de l'interface A_CHANGER" + System.lineSeparator()
                    + "public class MetierImpl implements A_CHANGER {" + System.lineSeparator()
                    + System.lineSeparator();

            for (String raw : entityLines) {
                String line = raw.trim().replace(" ", "");
                if (line.startsWith("c:")) {
                    String name = line.substring(2);
                    metierSrc += "    private " + name + "Dao " + name.toLowerCase() + "Dao;" + System.lineSeparator();
                }

            }
            metierSrc += System.lineSeparator()
                    + "    public MetierImpl() {" + System.lineSeparator();
            for (String raw : entityLines) {
                String line = raw.trim().replace(" ", "");
                if (line.startsWith("c:")) {
                    String name = line.substring(2);
                    metierSrc += "        this." + name.toLowerCase() + "Dao = DaoFactory.fabriquer" + name + "Dao();"
                            + System.lineSeparator();
                }
            }
            metierSrc += "    }" + System.lineSeparator()
                    + System.lineSeparator()
                    + "        public void init() {" + System.lineSeparator()
                    + "        Path path = Path.of(FacadeMetierImpl.class.getResource(\"/csv/init.csv\").toURI());"
                    + System.lineSeparator()
                    + "        for (String line : Files.readAllLines(path)) {" + System.lineSeparator()
                    + "            String[] tokens = line.split(\";\");" + System.lineSeparator()
                    + "            String name = tokens[0];" + System.lineSeparator()
                    + "            String val2 = tokens[1];" + System.lineSeparator()
                    + "            switch (name) {" + System.lineSeparator()
                    + "                case \"TODO\" -> {}" + System.lineSeparator()
                    + "                case \"TODO\" -> {}" + System.lineSeparator()
                    + "            }" + System.lineSeparator()
                    + "        }" + System.lineSeparator()
                    + "    }" + System.lineSeparator()
                    + System.lineSeparator()

                    + "}" + System.lineSeparator();

            Files.write(metierDirImpl.resolve("MetierImpl.java"), metierSrc.getBytes(StandardCharsets.UTF_8));

            // 2.6 Création de la classe DaoFactory.java
            String daoFactorySrc = "package dao;" + System.lineSeparator()
                    + System.lineSeparator()
                    + "import lombok.*;" + System.lineSeparator()
                    + System.lineSeparator()
                    + "import model.dao.*;" + System.lineSeparator()
                    + System.lineSeparator()
                    + "@NoArgsConstructor(access = AccessLevel.PRIVATE)" + System.lineSeparator()
                    + "public final class DaoFactory {" + System.lineSeparator()
                    + System.lineSeparator();
            for (String raw : entityLines) {
                String line = raw.trim().replace(" ", "");
                if (line.startsWith("c:")) {
                    String name = line.substring(2);
                    daoFactorySrc += "    private static " + name + "Dao instance" + name + "Dao;"
                            + System.lineSeparator();
                }
            }
            daoFactorySrc += System.lineSeparator();
            for (String raw : entityLines) {
                String line = raw.trim().replace(" ", "");
                if (line.startsWith("c:")) {
                    String name = line.substring(2);
                    daoFactorySrc += "    public static " + name + "Dao fabriquer" + name + "Dao(){"
                            + System.lineSeparator()
                            + "        if(instance" + name + "Dao == null){" + System.lineSeparator()
                            + "            instance" + name + "Dao = new " + name + "DaoImpl();"
                            + System.lineSeparator()
                            + "        }" + System.lineSeparator()
                            + "        return instance" + name + "Dao;" + System.lineSeparator()
                            + "    }" + System.lineSeparator();
                }
            }
            daoFactorySrc += System.lineSeparator()
                    + "}" + System.lineSeparator();
            Files.write(daoDir.resolve("DaoFactory.java"), daoFactorySrc.getBytes(StandardCharsets.UTF_8));

            // 3. Modify persistence.xml
            Path persistenceFile = tmpDir.resolve("src/main/resources/META-INF/persistence.xml");
            if (!Files.exists(persistenceFile)) {
                System.err.println("ERREUR : persistence.xml not found.");
                System.exit(1);
            }

            Path bddConf = cwd.resolve("bddConf.txt");
            if (!Files.exists(bddConf)) {
                System.err.println("ERREUR : bddConf.txt not found.");
                System.exit(1);
            }

            List<String> conf = Files.readAllLines(bddConf, StandardCharsets.UTF_8);
            if (conf.isEmpty()) {
                System.err.println("ERREUR : bddConf.txt est vide.");
                System.exit(1);
            }

            final String[] dbName = { "" };
            final String[] unitName = { "" };

            try {
                unitName[0] = conf.get(0).split("=")[1].trim();
                dbName[0] = conf.get(1).split("=")[1].trim();
            } catch (ArrayIndexOutOfBoundsException e) {
                System.err.println("ERREUR : bddConf.txt est mal formé.");
                System.exit(1);
            }

            if (unitName[0].isEmpty()) {
                System.err.println("AVERTISSEMENT : UNIT_NAME est vide.");
                System.exit(1);
            }
            if (dbName[0].isEmpty()) {
                System.err.println("AVERTISSEMENT : DB_NAME est vide.");
                System.exit(1);
            }

            List<String> lines = Files.readAllLines(persistenceFile, StandardCharsets.UTF_8);
            List<String> modified = lines.stream()
                    .map(l -> {
                        String[] tokens = l.split(" ");
                        for (int i = 0; i < tokens.length; i++) {
                            if (tokens[i].equals("name=\"PERSISTENCEUNITNAME\">")) {
                                tokens[i] = "name=\"" + unitName[0] + "\">";
                            }
                            if (tokens[i].contains("DBNAME")) {
                                tokens[i] = tokens[i].replace("DBNAME", dbName[0]);
                            }
                        }
                        return String.join(" ", tokens);
                    })
                    .collect(Collectors.toList());
            Files.write(persistenceFile, modified, StandardCharsets.UTF_8);

            // 4. In /tmp remove all files named "none.none"
            Files.walkFileTree(tmpDir, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    if (file.getFileName().toString().equals("none")) {
                        Files.delete(file);
                    }
                    return FileVisitResult.CONTINUE;
                }
            });

            // 5. Copy tmp content to the inteliji project folder
            // The project folder is the content of intellijProjectPath.txt
            // 5. Copier le contenu de tmp vers le dossier du projet IntelliJ
            Path intellijPathFile = cwd.resolve("inteliJProjectPath.txt");
            if (!Files.exists(intellijPathFile)) {
                System.err.println("ERREUR : intellijProjectPath.txt introuvable.");
                System.out.println("Le dossier tmp/ (le template customisé) à quand même été généré.");
                System.exit(1);
            }
            List<String> projLines = Files.readAllLines(intellijPathFile, StandardCharsets.UTF_8);
            if (projLines.isEmpty() || projLines.get(0).trim().isEmpty()) {
                System.err.println("ERREUR : intellijProjectPath.txt est vide ou contient un chemin invalide.");
                System.out.println("Le dossier tmp/ (le template customisé) à quand même été généré.");
                System.exit(1);
            }
            Path projectDir = Paths.get(projLines.get(0).trim()).toAbsolutePath().normalize();
            if (!Files.exists(projectDir) || !Files.isDirectory(projectDir)) {
                System.err.println("ERREUR : le dossier projet IntelliJ spécifié n'existe pas : " + projectDir);
                System.out.println("Le dossier tmp/ (le template customisé) à quand même été généré.");
                System.exit(1);
            }

            // Copier tout le contenu de tmp dans le dossier IntelliJ (écrase si nécessaire)
            copyDirectory(tmpDir, projectDir);

            System.out.println();
            System.out.println();
            System.out.println("-----------");
            System.out.println(". TERMINÉ .");
            System.out.println("-----------");

        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
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
}
