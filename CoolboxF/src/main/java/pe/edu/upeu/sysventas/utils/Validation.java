package pe.edu.upeu.sysventas.utils;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Validador sencillo que reemplaza a Hibernate Validator.
 *
 * <p>Hibernate Validator no funciona dentro del ejecutable nativo (GraalVM native image).
 * Esta clase ofrece la misma forma de uso que necesitan los controladores
 * ({@code Validation.buildDefaultValidatorFactory().getValidator().validate(obj)}) y lee las
 * mismas anotaciones del modelo: {@code @NotNull}, {@code @NotBlank}, {@code @Pattern},
 * {@code @Size}, {@code @Positive}, {@code @PositiveOrZero} y {@code @Email}.
 */
public final class Validation {

    private static final java.util.regex.Pattern EMAIL =
            java.util.regex.Pattern.compile("^[A-Za-z0-9._%+\\-]+@[A-Za-z0-9\\-]+(\\.[A-Za-z0-9\\-]+)+$");

    private Validation() {
    }

    public static ValidatorFactory buildDefaultValidatorFactory() {
        return new ValidatorFactory();
    }

    public static final class ValidatorFactory {
        public Validator getValidator() {
            return new Validator();
        }

        public void close() {
        }
    }

    public static final class Validator {

        public <T> Set<ConstraintViolation<T>> validate(T objeto) {
            Set<ConstraintViolation<T>> violaciones = new LinkedHashSet<>();
            if (objeto == null) {
                return violaciones;
            }
            for (Class<?> c = objeto.getClass(); c != null && c != Object.class; c = c.getSuperclass()) {
                for (Field campo : c.getDeclaredFields()) {
                    if (campo.getAnnotations().length == 0) {
                        continue;
                    }
                    Object valor;
                    try {
                        campo.setAccessible(true);
                        valor = campo.get(objeto);
                    } catch (Exception e) {
                        continue;
                    }
                    comprobar(objeto, campo, valor, violaciones);
                }
            }
            return violaciones;
        }

        private <T> void comprobar(T raiz, Field campo, Object valor, Set<ConstraintViolation<T>> out) {
            String nombre = campo.getName();

            NotNull notNull = campo.getAnnotation(NotNull.class);
            if (notNull != null && valor == null) {
                out.add(new ConstraintViolation<>(raiz, nombre, valor, notNull.message()));
            }

            NotBlank notBlank = campo.getAnnotation(NotBlank.class);
            if (notBlank != null && (valor == null || valor.toString().trim().isEmpty())) {
                out.add(new ConstraintViolation<>(raiz, nombre, valor, notBlank.message()));
            }

            Pattern pattern = campo.getAnnotation(Pattern.class);
            if (pattern != null && valor != null
                    && !java.util.regex.Pattern.compile(pattern.regexp()).matcher(valor.toString()).matches()) {
                out.add(new ConstraintViolation<>(raiz, nombre, valor, pattern.message()));
            }

            Size size = campo.getAnnotation(Size.class);
            if (size != null && valor != null) {
                int largo = valor instanceof Collection<?> col ? col.size() : valor.toString().length();
                if (largo < size.min() || largo > size.max()) {
                    out.add(new ConstraintViolation<>(raiz, nombre, valor, size.message()));
                }
            }

            Positive positive = campo.getAnnotation(Positive.class);
            if (positive != null) {
                BigDecimal n = aNumero(valor);
                if (n != null && n.signum() <= 0) {
                    out.add(new ConstraintViolation<>(raiz, nombre, valor, positive.message()));
                }
            }

            PositiveOrZero positiveOrZero = campo.getAnnotation(PositiveOrZero.class);
            if (positiveOrZero != null) {
                BigDecimal n = aNumero(valor);
                if (n != null && n.signum() < 0) {
                    out.add(new ConstraintViolation<>(raiz, nombre, valor, positiveOrZero.message()));
                }
            }

            Email email = campo.getAnnotation(Email.class);
            if (email != null && valor != null && !valor.toString().isEmpty()
                    && !EMAIL.matcher(valor.toString()).matches()) {
                out.add(new ConstraintViolation<>(raiz, nombre, valor, email.message()));
            }
        }

        private static BigDecimal aNumero(Object valor) {
            if (valor == null) {
                return null;
            }
            try {
                return new BigDecimal(valor.toString());
            } catch (NumberFormatException e) {
                return null;
            }
        }
    }

    public static final class ConstraintViolation<T> {
        private final T rootBean;
        private final String propertyPath;
        private final Object invalidValue;
        private final String message;

        ConstraintViolation(T rootBean, String propertyPath, Object invalidValue, String message) {
            this.rootBean = rootBean;
            this.propertyPath = propertyPath;
            this.invalidValue = invalidValue;
            this.message = message;
        }

        public String getMessage() {
            return message;
        }

        /** Nombre del campo; los controladores solo usan {@code getPropertyPath().toString()}. */
        public String getPropertyPath() {
            return propertyPath;
        }

        public Object getInvalidValue() {
            return invalidValue;
        }

        public T getRootBean() {
            return rootBean;
        }
    }
}