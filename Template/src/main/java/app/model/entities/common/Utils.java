package app.model.entities.common;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Utils {

    public static <T> List<T> iterableToList(Iterable<T> it){
        List<T> list = new ArrayList<>();
        for (T t : it){
            list.add(t);
        }
        return list;
    }
}
