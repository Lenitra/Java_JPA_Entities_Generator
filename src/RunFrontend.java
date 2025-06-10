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

public class RunFrontend {
    public static void main(String[] args) {
        try {
            Path cwd = Paths.get(".").toAbsolutePath().normalize();
            Path tmpDir = cwd.resolve("tmp/frontend");
            Path propertiesFile = tmpDir.resolve("src/main/resources/application.properties");
            deleteDirectory(tmpDir);

            Path templateDir = cwd.resolve("Template/Frontend");
            if (!Files.isDirectory(templateDir)) {
                showError("Dossier 'Template' introuvable");
                System.exit(1);
            }
            copyDirectory(templateDir, tmpDir);

            deleteNoneFiles(tmpDir);

            applyProjectSettings(cwd.resolve("projectSettings.txt"), propertiesFile, tmpDir);
        } catch (Exception e) {
            // TODO: handle exception
        }
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

        String ideKey = "project_path";
        if (configs.containsKey(ideKey) && !configs.get(ideKey).isEmpty()) {
            Path idePath = Paths.get(configs.get(ideKey) + "/frontend");
            if (!idePath.isAbsolute()) {
                showError("project_path doit etre un chemin absolu");
                return;
            }
            copyDirectory(tmpDir, idePath);
        } else if (configs.containsKey(ideKey))
            showWarn("project_path vide, le template genere se trouve dans /tmp");
        else
            showWarn("Clé 'project_path' non trouvée");

        showInfo("projectSettings appliqués");
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

}