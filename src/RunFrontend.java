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
            Path entityDir = tmpDir.resolve("src/entities/");
            Path entitiesFile = cwd.resolve("entities.txt");
            Path pagesPath = tmpDir.resolve("src/views/");
            deleteDirectory(tmpDir);

            Path templateDir = cwd.resolve("Template/Frontend");
            if (!Files.isDirectory(templateDir)) {
                showError("Dossier 'Template' introuvable");
                System.exit(1);
            }

            copyDirectory(templateDir, tmpDir);

            List<String> lines = Files.readAllLines(entitiesFile, StandardCharsets.UTF_8);

            for (int i = 0; i < lines.size(); i++) {
                String raw = lines.get(i).trim();
                if (raw.startsWith("c:")) {
                    String name = raw.substring(2).trim();
                    generateEntity(name, lines, i, entityDir);
                    i = skipAttributes(lines, i);
                }
            }
            generationVue(lines, pagesPath);
            deleteNoneFiles(tmpDir);
            generateNavBar(lines, tmpDir.resolve("src/components/"));
            setupRouter(lines);
            applyProjectSettings(cwd.resolve("projectSettings.txt"), tmpDir);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static int skipAttributes(List<String> lines, int idxStart) {
        int j = idxStart + 1;
        while (j < lines.size() && lines.get(j).trim().startsWith("-"))
            j++;
        return j - 1;
    }

    private static void applyProjectSettings(Path settingsFile, Path tmpDir) throws IOException {
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

    private static void generateEntity(String name, List<String> lines, int idxStart, Path entityDir)
            throws IOException {
        String nl = System.lineSeparator();
        StringBuilder sb = new StringBuilder();
        String tableName = name.toLowerCase();
        List<String> starred = new ArrayList<>();
        List<String> fieldLines = new ArrayList<>();
        Pattern camelCaseBoundary = Pattern.compile("([a-z])([A-Z])");
        Pattern genericPattern = Pattern.compile("<(.+?)>");
        String customGetterSetter = "";

        // Types basiques reconnus
        Set<String> basicTypes = Set.of(
                "String", "byte", "short", "int", "long", "float", "double",
                "Byte", "Short", "Integer", "Long", "Float", "Double",
                "Boolean", "Character", "LocalDate", "LocalDateTime");

        fieldLines.add("export default class " + name + " {");
        fieldLines.add("");
        fieldLines.add("    static endpoint = 'http://localhost:8080/api/v1/" + tableName + "/" + "';");
        fieldLines.add("");
        fieldLines.add("    static fields = [");
        fieldLines.add("        { name: 'id', type: 'number', label: 'ID', readonly: true },");

        // 1. Collecte des attributs
        int j = idxStart + 1;
        while (j < lines.size() && lines.get(j).trim().startsWith("-")) {
            String raw = lines.get(j).trim().substring(1).trim();
            boolean required = raw.contains("*");
            boolean manyToMany = false;
            boolean notNull = false;
            if (raw.contains("*") || raw.contains(".nn")) {
                notNull = true;
            }
            if (raw.contains(".mtm")) {
                manyToMany = true;
            }
            if (raw.contains(".otm")) {
                manyToMany = false;
            }
            if (required)
                raw = raw.replace("*", "").trim();

            String[] parts = raw.split("\\s+");
            if (parts.length < 2) {
                j++;
                continue;
            }
            String type = parts[0];
            String var = parts[1];

            String minVal = null, maxVal = null;
            for (int k = 2; k < parts.length; k++) {
                String tok = parts[k];
                if (tok.startsWith("-") && tok.length() > 1)
                    minVal = tok.substring(1);
                if (tok.startsWith("+") && tok.length() > 1)
                    maxVal = tok.substring(1);
            }

            String col = camelCaseBoundary.matcher(var).replaceAll("$1_$2").toLowerCase();
            String nullable = required ? ", nullable=false" : "";
            Matcher m = genericPattern.matcher(type);

            if ("String".equals(type)) {
                fieldLines.add("        {name: '" + col + "', type: 'text', label: '" + var + "'},");

            } else if (type.matches("(?:byte|short|int|long|float|double|Byte|Short|Integer|Long|Float|Double)")) {
                fieldLines.add("        {name: '" + col + "', type: 'number', label: '" + var + "' ");
                if (minVal != null)
                    fieldLines.set(fieldLines.size() - 1, fieldLines.get(fieldLines.size() - 1) + ", min: " + minVal);
                if (maxVal != null)
                    fieldLines.set(fieldLines.size() - 1, fieldLines.get(fieldLines.size() - 1) + ", max: " + maxVal);

                fieldLines.set(fieldLines.size() - 1, fieldLines.get(fieldLines.size() - 1) + "},");
            } else if (type.equals("Boolean") || type.equals("boolean")) {
                fieldLines.add("        {name: '" + col + "', type: 'checkbox', label: '" + var + "'},");
            } else if (type.equals("Character") || type.equals("char")) {
                fieldLines.add(
                        "        {name: '" + col + "', type: 'text', label: '" + var + "', maxLength: 1},");
            } else if (type.equals("LocalDate") || type.equals("LocalDateTime")) {
                fieldLines.add("        {name: '" + col + "', type: 'date', label: '" + var + "'},");
            } else {
                fieldLines.add("//TODO   : " + type + " " + var);
            }

            if (required)
                starred.add(var);

            j++;
        }
        sb.append(String.join(nl, fieldLines)).append(nl);
        sb.append("    ];").append(nl).append(nl);
        sb.append("}").append(nl);

        Files.write(entityDir.resolve(name + ".ts"), sb.toString().getBytes(StandardCharsets.UTF_8));

        showInfo("Entité générée: " + name);
    }

    private static void showInfo(String msg) {
        System.out.println("[INFO] " + msg);
    }

    private static void showError(String msg) {
        System.err.println(
                "******************************************************* ERROR *******************************************************");
        System.err.println("[ERROR] " + msg + "\n");

    }

    private static void showWarn(String msg) {
        System.out.println("[WARN] " + msg);
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

    private static void setupRouter(List<String> lines) throws IOException {
        Path cwd = Paths.get(".").toAbsolutePath().normalize();
        Path entitiesFile = cwd.resolve("entities.txt");
        Path routerFile = cwd.resolve("tmp/frontend/src/router/index.js");

        List<String> entityLines = Files.readAllLines(entitiesFile, StandardCharsets.UTF_8);
        List<String> entities = new ArrayList<>();
        for (String line : entityLines) {
            if (line.trim().startsWith("c:")) {
                String name = line.trim().substring(2).trim();
                entities.add(name);
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append("import { createRouter, createWebHistory } from 'vue-router'\n");
        for (String entity : entities) {
            sb.append("import ").append(entity).append("CRUD from '@/views/").append(entity).append("CRUD.vue'\n");
        }
        sb.append("\nconst routes = [\n");
        sb.append("    { path: '/', component: '/home' },\n");
        for (String entity : entities) {
            sb.append("    { path: '/").append(entity.toLowerCase()).append("', component: ").append(entity)
                    .append("CRUD },\n");

        }
        sb.append("]\n\n");
        sb.append("const router = createRouter({\n");
        sb.append("    history: createWebHistory(),\n");
        sb.append("    routes\n");
        sb.append("})\n\n");
        sb.append("export default router\n");

        Files.write(routerFile, sb.toString().getBytes(StandardCharsets.UTF_8));
        showInfo("Fichier router/index.js généré avec toutes les entités");
    }

    private static void generationVue(List<String> lines, Path vueDir) {
        String nl = System.lineSeparator();
        for (String line : lines) {
            if (line.trim().startsWith("c:")) {
                String name = line.trim().substring(2).trim();
                StringBuilder sb = new StringBuilder();

                sb.append("<template>").append(nl);
                sb.append("  <GenericCrud").append(nl);
                sb.append("      :entityConfig=\"").append(name).append("\"").append(nl);
                sb.append("      title=\"").append(name).append("\"").append(nl);
                sb.append("  />").append(nl);
                sb.append("</template>").append(nl).append(nl);

                sb.append("<script setup>").append(nl);
                sb.append("import GenericCrud from '@/components/GenericCrud.vue'").append(nl);
                sb.append("import ").append(name)
                        .append(" from \"@/entities/").append(name).append(".js\";")
                        .append(nl);
                sb.append("</script>").append(nl);

                try {
                    Files.createDirectories(vueDir);
                    Files.write(
                            vueDir.resolve(name + "CRUD.vue"),
                            sb.toString().getBytes(StandardCharsets.UTF_8));
                    showInfo("Vue CRUD généré pour: " + name);
                } catch (IOException e) {
                    showError("Erreur lors de la génération du fichier Vue pour "
                            + name + ": " + e.getMessage());
                }
            }
        }
    }

    private static void generateNavBar(List<String> lines, Path vueDir) {
        String nl = System.lineSeparator();
        StringBuilder sb = new StringBuilder();
        sb.append("<template>").append(nl);
        sb.append("    <nav>").append(nl);
        sb.append("        <ul>").append(nl);

        for (String line : lines) {
            if (line.trim().startsWith("c:")) {
                String name = line.trim().substring(2).trim();
                sb.append("            <li><router-link to=\"/")
                        .append(name.toLowerCase())
                        .append("\">")
                        .append(name)
                        .append("</router-link></li>")
                        .append(nl);
            }
        }

        sb.append("        </ul>").append(nl);
        sb.append("    </nav>").append(nl);
        sb.append("</template>").append(nl).append(nl);

        sb.append("<style scoped>").append(nl);
        // Fond clair et texte sombre
        sb.append("nav {").append(nl);
        sb.append("  background-color: #f5f5f5;").append(nl);
        sb.append("  padding: 0.5rem;").append(nl);
        sb.append("}").append(nl).append(nl);
        // Liste horizontale simple
        sb.append("nav ul {").append(nl);
        sb.append("  display: flex;").append(nl);
        sb.append("  gap: 1rem;").append(nl);
        sb.append("  margin: 0; padding: 0;").append(nl);
        sb.append("  list-style: none;").append(nl);
        sb.append("}").append(nl).append(nl);
        // Liens génériques
        sb.append("nav a {").append(nl);
        sb.append("  color: #333;").append(nl);
        sb.append("  text-decoration: none;").append(nl);
        sb.append("  padding: 0.25rem 0.5rem;").append(nl);
        sb.append("  border-radius: 3px;").append(nl);
        sb.append("}").append(nl).append(nl);
        // Hover et actif
        sb.append("nav a:hover {").append(nl);
        sb.append("  background-color: #e0e0e0;").append(nl);
        sb.append("}").append(nl).append(nl);
        sb.append("nav a.router-link-exact-active {").append(nl);
        sb.append("  font-weight: bold;").append(nl);
        sb.append("}").append(nl);
        sb.append("</style>").append(nl);

        try {
            Files.write(vueDir.resolve("NavBar.vue"),
                    sb.toString().getBytes(StandardCharsets.UTF_8));
            showInfo("NavBar.vue généré avec style simple");
        } catch (IOException e) {
            showError("Erreur lors de la génération de NavBar.vue: " + e.getMessage());
        }
    }

}