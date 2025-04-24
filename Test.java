import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.stream.Collectors;

public class Test {
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
            Path daoImplDir = daoDir.resolve("bdd");
            Path referenceDir = entityDir.resolve("references");
        
            Files.createDirectories(entityDir);
            Files.createDirectories(daoDir);
            Files.createDirectories(daoImplDir);
            Files.createDirectories(referenceDir);

            


            
            // 2. Process entities.txt
            Path entitiesFile = cwd.resolve("entities.txt");
            if (!Files.exists(entitiesFile)) {
                System.err.println("ERREUR : entities.txt not found.");
                System.exit(1);
            }
            List<String> entityLines = Files.readAllLines(entitiesFile, StandardCharsets.UTF_8);
            for (String raw : entityLines) {
                String line = raw.trim().replace(" ", "");
                
                if (line.startsWith("c:")) {
                    String name = line.substring(2);
                    // Entity class
                    String entitySrc = "package entities;" + System.lineSeparator()
                            + System.lineSeparator()
                            + "public class " + name + " extends AbstractEntity {" + System.lineSeparator()
                            + System.lineSeparator()
                            + "}" + System.lineSeparator();
                    Files.write(entityDir.resolve(name + ".java"), entitySrc.getBytes(StandardCharsets.UTF_8));

                    // DAO interface
                    String daoSrc = "package dao;" + System.lineSeparator()
                            + System.lineSeparator()
                            + "public interface " + name + "Dao extends Dao<" + name + "> {" + System.lineSeparator()
                            + "}" + System.lineSeparator();
                    Files.write(daoDir.resolve(name + "Dao.java"), daoSrc.getBytes(StandardCharsets.UTF_8));

                    // DAO implementation
                    String implSrc = "package dao.bdd;" + System.lineSeparator()
                            + System.lineSeparator()
                            + "import entities." + name + ";" + System.lineSeparator()
                            + "import dao." + name + "Dao;" + System.lineSeparator()
                            + System.lineSeparator()
                            + "public class " + name + "DaoImpl extends AbstractDaoImpl<" + name + "> implements "
                            + name + "Dao {" + System.lineSeparator()
                            + "}" + System.lineSeparator();
                    Files.write(daoImplDir.resolve(name + "DaoImpl.java"), implSrc.getBytes(StandardCharsets.UTF_8));

                } 
                
                else if (line.startsWith("e:")) {
                    String name = line.substring(2);
                    String enumSrc = "package entities.references;" + System.lineSeparator()
                            + System.lineSeparator()
                            + "public enum " + name + " {" + System.lineSeparator()
                            + "    // TODO: add values" + System.lineSeparator()
                            + "}" + System.lineSeparator();
                    Files.write(referenceDir.resolve(name + ".java"), enumSrc.getBytes(StandardCharsets.UTF_8));
                }
            }




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

            String unitName = conf.get(0).trim();
            String dbName = conf.size() >= 2 ? conf.get(1).trim() : "";
            if (unitName.isEmpty()) {
                System.err.println("AVERTISSEMENT : UNIT_NAME est vide.");
            }
            if (dbName.isEmpty()) {
                System.err.println("AVERTISSEMENT : DB_NAME est vide.");
            }

            List<String> lines = Files.readAllLines(persistenceFile, StandardCharsets.UTF_8);
            List<String> modified = lines.stream()
                    .map(l -> {
                        String[] tokens = l.split(" ");
                        for (int i = 0; i < tokens.length; i++) {
                            if (tokens[i].equals("name=\"PERSISTENCEUNITNAME\">")) {
                                tokens[i] = "name=\"" + unitName + "\">";
                            }
                        }
                        return String.join(" ", tokens);
                    })
                    .collect(Collectors.toList());
            Files.write(persistenceFile, modified, StandardCharsets.UTF_8);



            System.out.println("TERMINE");


            
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
