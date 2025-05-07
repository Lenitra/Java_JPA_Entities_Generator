package app.model.entities.common;

import jakarta.validation.*;
import lombok.NoArgsConstructor;

import java.util.Set;
import java.util.stream.Collectors;

@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public final class ValidUtil {
    private static final ValidatorFactory vf = Validation.buildDefaultValidatorFactory();
    private static final Validator v = vf.getValidator();

    public static <T> T isValide(T objectValid) throws ValidException {
        if(objectValid == null){
            throw new ValidException("L'objet est null");
        }

        Set<ConstraintViolation<T>> setC = v.validate(objectValid);

        if (!setC.isEmpty()) {
            throw new ValidException(setC.stream().map(ConstraintViolation::getMessage).collect(Collectors.joining(",\n")));
        }

        return objectValid;
    }


}
