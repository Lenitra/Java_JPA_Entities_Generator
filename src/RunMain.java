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
            generateEntityFactory(lines, entityDir);

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

    private static void generateEntityFactory(List<String> lines, Path entityDir) throws IOException {
        String nl = System.lineSeparator();
        StringBuilder sb = new StringBuilder();

        // en-tête de la factory
        sb.append("package app.model.entities;").append(nl).append(nl)
                .append("import app.model.entities.enums.*;").append(nl)
                .append("import app.model.entities.common.*;").append(nl)
                .append("import java.time.LocalDate;").append(nl)
                .append("import lombok.AccessLevel;").append(nl)
                .append("import lombok.NoArgsConstructor;").append(nl).append(nl)
                .append("@NoArgsConstructor(access = AccessLevel.PRIVATE)").append(nl).append(nl)
                .append("public final class EntityFactory {").append(nl).append(nl);

        // pour chaque entité déclarée dans entities.txt
        for (int i = 0; i < lines.size(); i++) {
            String raw = lines.get(i).trim();
            if (!raw.startsWith("c:"))
                continue;

            String name = raw.substring(2).trim();
            List<String> params = new ArrayList<>();
            List<String> setters = new ArrayList<>();

            // collecte des attributs qui suivent, en sautant les List/Set/Map/Collection
            int j = i + 1;
            while (j < lines.size() && lines.get(j).trim().startsWith("-")) {
                String attr = lines.get(j).trim().substring(1).trim();
                if (attr.endsWith("*")) {
                    attr = attr.substring(0, attr.length() - 1).trim();
                }
                String[] parts = attr.split("\\s+");
                if (parts.length >= 2) {
                    String type = parts[0];
                    String var = parts[1];
                    // ne pas gérer les collections
                    if (type.startsWith("List") ||
                            type.startsWith("Set") ||
                            type.startsWith("Map") ||
                            type.startsWith("Collection")) {
                        j++;
                        continue;
                    }
                    params.add(type + " " + var);
                    String method = "set" + Character.toUpperCase(var.charAt(0)) + var.substring(1);
                    setters.add("        obj." + method + "(" + var + ");");
                }
                j++;
            }

            // génération de la méthode createNomEntité(...)
            sb.append("    public static ").append(name)
                    .append(" create").append(name).append("(")
                    .append(String.join(", ", params)).append(") throws ValidException {").append(nl)
                    .append("        ").append(name).append(" obj = new ").append(name).append("();").append(nl);
            for (String s : setters) {
                sb.append(s).append(nl);
            }
            sb.append("        return ValidUtil.isValide(obj);").append(nl)
                    .append("    }").append(nl).append(nl);

            // on saute les lignes d'attributs
            i = j - 1;
        }

        sb.append("}").append(nl);

        // écriture du fichier EntityFactory.java
        Files.write(entityDir.resolve("EntityFactory.java"),
                sb.toString().getBytes(StandardCharsets.UTF_8));
        showInfo("EntityFactory générée");
    }

    /**
     * Vérifie si le type commence par "List" ou "Set" et si un type générique est
     * présent.
     * 
     * On entre dans cette condition lorsque le type de l'attribut est une
     * collection
     * (par exemple, List ou Set) avec un type générique défini (par exemple,
     * List<Entité> ou Set<Entité>).
     * Cela permet de gérer les relations OneToMany unidirectionnelles dans une
     * entité JPA.
     * 
     * Exemple :
     * - Si le type est "List<Entité>", cela signifie qu'il s'agit d'une relation
     * OneToMany.
     * - Si le type est "Set<Entité>", cela signifie également une relation
     * OneToMany, mais sans ordre garanti.
     */
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

        // 1. Collecte des attributs
        int j = idxStart + 1;
        while (j < lines.size() && lines.get(j).trim().startsWith("-")) {
            String raw = lines.get(j).trim().substring(1).trim();
            boolean required = raw.contains("*");
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


            if (type.startsWith("Map") && m.find()) {
                String[] kv = m.group(1).split(",");
                String kt = kv[0].trim();
                String vt = kv[1].trim();
                boolean keyBasic = basicTypes.contains(kt);
                boolean valBasic = basicTypes.contains(vt);
                String tbl = tableName + "_" + var;
                String fk = tableName + "_id";

                // 1) clé et valeur = entités
                if (!keyBasic && !valBasic) {
                    fieldLines.add("    @OneToMany(fetch = FetchType.LAZY)");
                    fieldLines.add("    @JoinTable( name = \"" + tableName + "_" + var + "\",");
                    fieldLines.add("            joinColumns = @JoinColumn(name = \"" + tableName + "_id\", foreignKey = @ForeignKey(name = \"fk__" + tableName + "_" + var + "__" + tableName + "_id\")),");
                    fieldLines.add("            inverseJoinColumns = @JoinColumn(name = \"" + var + "_id\", foreignKey = @ForeignKey(name = \"fk__" + tableName + "_" + var + "__" + var + "_id\")))");
                    fieldLines.add("    @MapKeyJoinColumn(name = \"" + var + "_id\", foreignKey = @ForeignKey(name = \"fk__" + tableName + "_" + var + "_id\"))");
                    fieldLines.add("    private Map<" + kt + ", " + vt + "> " + var + " = new HashMap<>();");
                    fieldLines.add("");
                    customGetterSetter += "    public void putTo" + Character.toUpperCase(var.charAt(0))
                            + var.substring(1)
                            + "(" + kt + " key, " + vt + " value) {" + nl
                            + "        if (key == null || value == null) {" + nl
                            + "            throw new IllegalArgumentException(\"Key ou Value ne peuvent pas etre null\");"
                            + nl
                            + "        }" + nl
                            + "        this." + var + ".put(key, value);" + nl
                            + "    }" + nl + nl;
                    customGetterSetter += "    public Map<" + kt + ", " + vt + "> get"
                            + Character.toUpperCase(var.charAt(0)) + var.substring(1) + "() {" + nl
                            + "        return Collections.unmodifiableMap(this." + var + ");" + nl
                            + "    }" + nl + nl;
                }
                
                // 2) clé entité, valeur basique
                else if (!keyBasic && valBasic) {
                    fieldLines.add("    @ElementCollection(fetch = FetchType.LAZY)");
                    fieldLines.add("    @CollectionTable(name = \"" + tbl + "\", joinColumns = @JoinColumn(name = \""
                            + fk + "\", foreignKey = @ForeignKey(name = \"fk__"
                            + tableName + "__" + var + "_id\")))");
                    fieldLines
                            .add("    @MapKeyJoinColumn(name = \"" + var + "_id\"," +
                                    " foreignKey = @ForeignKey(name = \"fk__" + tableName + "__" + var + "_id\"), nullable=false)");
                    fieldLines.add("    @Column(name = \"" + var + "\", nullable=false)");
                    fieldLines.add("    private Map<" + kt + ", " + vt + "> " + var + " = new HashMap<>();");

                    customGetterSetter += "    public void putTo" + Character.toUpperCase(var.charAt(0))
                            + var.substring(1)
                            + "(" + kt + " key, " + vt + " value) {" + nl
                            + "        if (key == null || value == null) {" + nl
                            + "            throw new IllegalArgumentException(\"Key ou Value ne peuvent pas etre null\");"
                            + nl
                            + "        }" + nl
                            + "        this." + var + ".put(key, value);" + nl
                            + "    }" + nl + nl;
                    customGetterSetter += "    public Map<" + kt + ", " + vt + "> get"
                            + Character.toUpperCase(var.charAt(0)) + var.substring(1) + "() {" + nl
                            + "        return Collections.unmodifiableMap(this." + var + ");" + nl
                            + "    }" + nl + nl;
                }

                // 3) clé basique, valeur entité
                else if (keyBasic && !valBasic) {
                    fieldLines.add("    @OneToMany(fetch = FetchType.LAZY)");
                    fieldLines.add("    @MapKeyColumn(name = \"" + var + "_key\")");
                    fieldLines.add("    @JoinColumn(name = \"" + fk + "\", foreignKey = @ForeignKey(name = \"fk_"
                            + tableName + "_" + var + "\"))");
                    fieldLines.add("    private Map<" + kt + ", " + vt + "> " + var + " = new HashMap<>();");

                    customGetterSetter += "    public void putTo" + Character.toUpperCase(var.charAt(0))
                            + var.substring(1)
                            + "(" + kt + " key, " + vt + " value) {" + nl
                            + "        if (key == null || value == null) {" + nl
                            + "            throw new IllegalArgumentException(\"Key ou Value ne peuvent pas etre null\");"
                            + nl
                            + "        }" + nl
                            + "        this." + var + ".put(key, value);" + nl
                            + "    }" + nl + nl;
                    customGetterSetter += "    public Map<" + kt + ", " + vt + "> get"
                            + Character.toUpperCase(var.charAt(0)) + var.substring(1) + "() {" + nl
                            + "        return Collections.unmodifiableMap(this." + var + ");" + nl
                            + "    }" + nl + nl;
                }
                
                // 4) deux types basiques
                else {
                    fieldLines.add("    @ElementCollection(fetch = FetchType.LAZY)");
                    fieldLines.add("    @CollectionTable(name = \"" + tbl + "\", joinColumns = @JoinColumn(name = \""
                            + fk + "\", foreignKey = @ForeignKey(name = \"fk_"
                            + tableName + "_" + var + "\")))");
                    fieldLines.add("    @MapKeyColumn(name = \"" + var + "_key\")");
                    fieldLines.add("    @Column(name = \"" + var + "_value\")");
                    fieldLines.add("    private Map<" + kt + ", " + vt + "> " + var + " = new HashMap<>();");

                    customGetterSetter += "    public void addTo" + Character.toUpperCase(var.charAt(0))
                            + var.substring(1)
                            + "(" + kt + " key, " + vt + " value) {" + nl
                            + "        if (key == null || value == null) {" + nl
                            + "            throw new IllegalArgumentException(\"Key ou Value ne peuvent pas etre null\");"
                            + nl
                            + "        }" + nl
                            + "        this." + var + ".put(key, value);" + nl
                            + "    }" + nl + nl;
                    customGetterSetter += "    public Map<" + kt + ", " + vt + "> get"
                            + Character.toUpperCase(var.charAt(0)) + var.substring(1) + "() {" + nl
                            + "        return Collections.unmodifiableMap(this." + var + ");" + nl
                            + "    }" + nl + nl;
                }

                j++;
                continue;
            }
            // 2. OneToMany unidirectionnel (List<Entité> ou Set<Entité>)
            if ((type.startsWith("List") || type.startsWith("Set")) && m.find()) {
                String elt = m.group(1).trim();
                if (minVal != null || maxVal != null) {
                    StringBuilder sizeAnn = new StringBuilder("    @Size(");
                    if (minVal != null)
                        sizeAnn.append("min = ").append(minVal);
                    if (minVal != null && maxVal != null)
                        sizeAnn.append(", ");
                    if (maxVal != null)
                        sizeAnn.append("max = ").append(maxVal);
                    sizeAnn.append(", message = \"La taille doit etre comprise entre ").append(minVal).append(" et ")
                            .append(maxVal).append("\")");
                    fieldLines.add(sizeAnn.toString());
                }
                String fk = tableName + "_id";
                fieldLines
                        .add("    @OneToMany(fetch = FetchType.LAZY)");
                fieldLines.add("    @JoinColumn(name = \"" + fk + "\", foreignKey = @ForeignKey(name = \"fk_"
                        + tableName + "_" + camelCaseBoundary.matcher(var).replaceAll("$1_$2").toLowerCase() + "\"))");
                if (type.startsWith("List")) {
                    fieldLines.add("    @OrderColumn(name = \"" + var + "_order\")");
                }
                fieldLines.add("    private " + type + " " + var + " = new "
                        + (type.startsWith("List") ? "ArrayList<>()" : "HashSet<>()") + ";");
                fieldLines.add("");
                customGetterSetter += "    public void addTo" + Character.toUpperCase(var.charAt(0)) + var.substring(1)
                        + "(" + elt + " element) {" + nl
                        + "        if (element == null) {" + nl
                        + "            throw new IllegalArgumentException(\"On ajoute pas un element null\");" + nl
                        + "        }" + nl
                        + "        this." + var + ".add(element);" + nl
                        + "    }" + nl + nl;

                customGetterSetter += "    public " + type + " get" + Character.toUpperCase(var.charAt(0))
                        + var.substring(1) + "() {" + nl
                        + "        return Collections.unmodifiable" + (type.startsWith("List") ? "List" : "Set")
                        + "(this." + var + ");" + nl
                        + "    }" + nl + nl;
            }

            // 5. ElementCollection pour List<Type> ou Set<Type> de basiques
            else if ((type.startsWith("List") || type.startsWith("Set") || type.startsWith("Collection"))) {
                String coll = type.startsWith("List") ? "List" : "Set";
                String elt = m.find() ? m.group(1).trim() : "String";
                String tbl = tableName + "_" + var;
                String fk = tableName + "_id";
                fieldLines.add("    @ElementCollection(fetch = FetchType.LAZY)");
                fieldLines.add("    @CollectionTable(name = \"" + tbl + "\", joinColumns = @JoinColumn(name = \"" + fk
                        + "\", foreignKey = @ForeignKey(name = \"fk_" + tableName + "_" + var + "\")))");
                fieldLines.add("    private " + coll + "<" + elt + "> " + var + " = new "
                        + (coll.equals("List") ? "ArrayList<>()" : "HashSet<>()") + ";");
                fieldLines.add("");
                customGetterSetter += "    public void addTo" + Character.toUpperCase(var.charAt(0)) + var.substring(1)
                        + "(" + elt + " element) {" + nl
                        + "        if (element == null) {" + nl
                        + "            throw new IllegalArgumentException(\"On ajoute pas un element null\");" + nl
                        + "        }" + nl
                        + "        this." + var + ".add(element);" + nl
                        + "    }" + nl + nl;

                customGetterSetter += "    public " + coll + "<" + elt + "> get" + Character.toUpperCase(var.charAt(0))
                        + var.substring(1) + "() {" + nl
                        + "        return Collections.unmodifiable" + (coll.equals("List") ? "List" : "Set") + "(this."
                        + var + ");" + nl
                        + "    }" + nl + nl;
            }

            // Champ simple non-relationnel
            else {
                fieldLines.add("    @Getter @Setter");
                // 2. Bean Validation pour types simples
                if ("String".equals(type)) {
                    if (minVal != null || maxVal != null) {
                        StringBuilder sizeAnn = new StringBuilder("    @Size(");
                        if (minVal != null)
                            sizeAnn.append("min = ").append(minVal);
                        if (minVal != null && maxVal != null)
                            sizeAnn.append(", ");
                        if (maxVal != null)
                            sizeAnn.append("max = ").append(maxVal);
                        sizeAnn.append(", message = \"La taille doit etre comprise entre ").append(minVal)
                                .append(" et ").append(maxVal).append("\")");
                        fieldLines.add(sizeAnn.toString());
                    }
                    fieldLines.add("    @NotBlank(message = \"Ne peut pas etre vide\")");
                } else if (type.matches("(?:byte|short|int|long|float|double|Byte|Short|Integer|Long|Float|Double)")) {
                    if (minVal != null)
                        fieldLines.add("    @Min(value = " + minVal + ", message=\"La valeur minimale est :" + minVal
                                + " \")");
                    if (maxVal != null)
                        fieldLines.add("    @Max(value = " + maxVal + ", message=\"La valeur maximale est :" + maxVal
                                + " \")");
                }
                if (required)
                    fieldLines.add("    @NotNull(message = \"Ce champ ne peut pas etre null\")");

                // 3. Column avec longueur si applicable
                StringBuilder colAnn = new StringBuilder("    @Column(name = \"" + col + "\"");
                if (!type.matches("(?:byte|short|int|long|float|double|Byte|Short|Integer|Long|Float|Double)")
                        && maxVal != null) {
                    colAnn.append(", length = ").append(maxVal);
                }
                colAnn.append(nullable).append(")");
                fieldLines.add(colAnn.toString());

                // 4. Lombok getters/setters et champ
                fieldLines.add("    private " + type + " " + var + ";");
                fieldLines.add("");

                if (required)
                    starred.add(var);
            }
            j++;
        }

        // 4. Génération de l’en-tête & écriture du fichier
        sb.append("package app.model.entities;").append(nl).append(nl)
                .append("import jakarta.persistence.*;").append(nl)
                .append("import lombok.*;").append(nl)
                .append("import org.springframework.data.jpa.domain.AbstractPersistable;").append(nl)
                .append("import jakarta.validation.constraints.NotBlank;").append(nl)
                .append("import jakarta.validation.constraints.NotNull;").append(nl)
                .append("import java.util.*;").append(nl)
                .append("import app.model.entities.enums.*;").append(nl)
                .append("import java.time.LocalDate;").append(nl)
                .append("import jakarta.validation.constraints.Max;").append(nl)
                .append("import jakarta.validation.constraints.Min;").append(nl)
                .append("import jakarta.validation.constraints.Size;").append(nl)
                .append("import org.springframework.format.annotation.DateTimeFormat;").append(nl).append(nl)
                .append("@Entity").append(nl)
                .append("@Table(name=\"").append(tableName).append("\"");
        if (!starred.isEmpty()) {
            String cols = starred.stream()
                    .map(v -> "\"" + camelCaseBoundary.matcher(v).replaceAll("$1_$2").toLowerCase() + "\"")
                    .collect(Collectors.joining(", "));
            String uc = "uk_" + tableName + "_" + starred.stream()
                    .map(v -> camelCaseBoundary.matcher(v).replaceAll("$1_$2").toLowerCase())
                    .collect(Collectors.joining("_"));
            sb.append(", uniqueConstraints=@UniqueConstraint(name=\"")
                    .append(uc).append("\", columnNames={").append(cols).append("})");
        }
        sb.append(")").append(nl)
                .append("@NoArgsConstructor(access=AccessLevel.PROTECTED)").append(nl)
                .append("@ToString(callSuper=true");
        if (!starred.isEmpty()) {
            String of = starred.stream().map(v -> "\"" + v + "\"").collect(Collectors.joining(", "));
            sb.append(", of={").append(of).append("}");
        }
        sb.append(")").append(nl)
                .append("@EqualsAndHashCode(callSuper=false");
        if (!starred.isEmpty()) {
            String of = starred.stream().map(v -> "\"" + v + "\"").collect(Collectors.joining(", "));
            sb.append(", of={").append(of).append("}");
        }
        sb.append(")").append(nl).append(nl)
                .append("public class ").append(name).append(" extends AbstractPersistable<Long> {").append(nl)
                .append(nl);

        // Insertion des arguments
        for (String fld : fieldLines) {
            sb.append(fld).append(nl);
        }

        // Insertion des champs
        for (String fld : customGetterSetter.split("\n")) {
            sb.append(fld);
        }
        sb.append("}").append(nl);

        Files.write(entityDir.resolve(name + ".java"), sb.toString().getBytes(StandardCharsets.UTF_8));
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
                + "import app.model.services.interfaces.I" + name + "Service;" + System.lineSeparator()
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
        String src = "package app.model.commands;" + System.lineSeparator() + System.lineSeparator()
                + "import app.model.services.interfaces.I" + name + "Service;" + System.lineSeparator()
                + "import app.model.entities." + name + ";" + System.lineSeparator()
                + "import lombok.NonNull;" + System.lineSeparator()
                + "import lombok.RequiredArgsConstructor;" + System.lineSeparator()
                + "import org.jline.terminal.Terminal;" + System.lineSeparator()
                + "import org.springframework.shell.standard.ShellComponent;" + System.lineSeparator()
                + "import org.springframework.shell.standard.ShellMethod;" + System.lineSeparator()
                + "import app.model.services.exceptions.ServiceException;" + System.lineSeparator()
                + System.lineSeparator()
                + "@ShellComponent" + System.lineSeparator()
                + "@RequiredArgsConstructor" + System.lineSeparator()
                + "public class " + name + "Commands {" + System.lineSeparator()
                + "    @NonNull private final I" + name + "Service " + varName + ";" + System.lineSeparator()
                + "    @NonNull private final Terminal terminal;" + System.lineSeparator() + System.lineSeparator()
                + "    @ShellMethod(value = \"Permet de lister les " + name.toLowerCase() + "\", key = \""
                + name.toLowerCase() + "-list\")" + System.lineSeparator()
                + "    public void lister(){" + System.lineSeparator()
                + "        try {" + System.lineSeparator()
                + "            Iterable<" + name + "> les" + name + " = " + name.toLowerCase() + "Service.findAll();"
                + System.lineSeparator()
                + "            for (" + name + " obj : les" + name + "){" + System.lineSeparator()
                + "                terminal.writer().println(obj.toString());" + System.lineSeparator()
                + "            }" + System.lineSeparator()
                + "            terminal.writer().println(\"Nombre de " + name.toLowerCase() + " : \" + "
                + name.toLowerCase() + "Service.count());" + System.lineSeparator()
                + "            terminal.flush();" + System.lineSeparator()
                + "        } catch (ServiceException e) {throw new RuntimeException(e);}" + System.lineSeparator()
                + "    }" + System.lineSeparator()
                + "}";
        Files.write(commandsDir.resolve(name + "Commands.java"), src.getBytes(StandardCharsets.UTF_8));
        showInfo("Commands générés: " + name + "Commands");
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

        String ideKey = "intelij_project_path";
        if (configs.containsKey(ideKey) && !configs.get(ideKey).isEmpty()) {
            Path idePath = Paths.get(configs.get(ideKey));
            if (!idePath.isAbsolute()) {
                showError("intellij_project_path doit etre un chemin absolu");
                return;
            }
            copyDirectory(tmpDir, idePath);
        } else if (configs.containsKey(ideKey))
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
