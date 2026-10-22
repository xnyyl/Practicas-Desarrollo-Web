package mx.edu.backendacademico.model;

import java.util.Locale;
import java.util.Objects;

public record Alumno(
        Long id,
        String matricula,
        String nombre,
        String correo,
        String estatus
) {
    // Constructor compacto para validar y normalizar invariantes
    public Alumno {
        matricula = texto(matricula, 30, "matrícula").toUpperCase(Locale.ROOT);
        nombre = texto(nombre, 120, "nombre");
        correo = texto(correo, 160, "correo");
        Objects.requireNonNull(estatus, "El estatus es obligatorio");
    }

    private static String texto(String valor, int maxLongitud, String campo) {
        Objects.requireNonNull(valor, "El campo " + campo + " es obligatorio");
        String limpio = valor.trim();
        if (limpio.isEmpty()) {
            throw new IllegalArgumentException("El campo " + campo + " no puede estar vacío");
        }
        if (limpio.length() > maxLongitud) {
            throw new IllegalArgumentException("El campo " + campo + " excede la longitud máxima de " + maxLongitud);
        }
        return limpio;
    }
}