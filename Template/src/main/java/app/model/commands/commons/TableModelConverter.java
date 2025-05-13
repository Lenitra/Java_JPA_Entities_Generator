package guilde.models.commands.commons;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.shell.table.ArrayTableModel;
import org.springframework.shell.table.TableModel;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class TableModelConverter {

    public static TableModel convertToTableModel(List<?> entities, List<String> columns) {
        if (entities == null || entities.isEmpty()) {
            return new ArrayTableModel(new String[][]{{"No data"}});
        }

        List<List<Object>> rows = new ArrayList<>();

        // Récupérer les champs de l'entité, y compris ceux de toutes les superclasses
        Map<String, Field> allFields = getAllFieldsForColumns(entities.get(0).getClass(), columns);

        // Récupérer les noms des champs à afficher dans l'ordre spécifié
        List<String> headers = new ArrayList<>(columns);

        // Récupérer les valeurs des champs pour chaque entité
        for (Object entity : entities) {
            List<Object> row = new ArrayList<>();
            for (String column : columns) {
                try {
                    Field field = allFields.get(column);
                    field.setAccessible(true);
                    row.add(field.get(entity));
                } catch (IllegalAccessException e) {
                    row.add("Error");
                }
            }
            rows.add(row);
        }

        // Créer le tableau à partir des en-têtes et des lignes
        String[][] data = new String[rows.size() + 1][headers.size()];
        data[0] = headers.toArray(new String[0]);
        for (int i = 0; i < rows.size(); i++) {
            for (int j = 0; j < rows.get(i).size(); j++) {
                data[i + 1][j] = rows.get(i).get(j) != null ? rows.get(i).get(j).toString() : null;
            }
        }

        return new ArrayTableModel(data);
    }

    private static Map<String, Field> getAllFieldsForColumns(Class<?> type, List<String> columns) {
        Map<String, Field> fields = new HashMap<>();
        for (Field field : type.getDeclaredFields()) {
            if (columns.contains(field.getName())) {
                fields.put(field.getName(), field);
            }
        }
        Class<?> superClass = type.getSuperclass();
        if (superClass != null) {
            fields.putAll(getAllFieldsForColumns(superClass, columns));
        }
        return fields;
    }
}
