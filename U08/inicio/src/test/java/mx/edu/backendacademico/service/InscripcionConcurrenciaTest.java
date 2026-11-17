package mx.edu.backendacademico.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import mx.edu.backendacademico.dto.CrearAlumnoRequest;
import mx.edu.backendacademico.exception.OperacionAcademicaException;
import mx.edu.backendacademico.repository.jpa.InscripcionRepository;
import mx.edu.backendacademico.repository.jpa.MateriaEntity;
import mx.edu.backendacademico.repository.jpa.MateriaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class InscripcionConcurrenciaTest {

    @Autowired
    private InscripcionService inscripcionService;

    @Autowired
    private AlumnoService alumnoService;

    @Autowired
    private MateriaRepository materiaRepository;

    @Autowired
    private InscripcionRepository inscripcionRepository;

    @Test
    void admiteSoloUnAlumnoCuandoDosTareasCompitenPorElUltimoLugar() throws Exception {
        String sufijo = UUID.randomUUID().toString().substring(0, 6).toUpperCase();

        // 1. Preparar materia con cupo = 1 y dos alumnos activos distintos
        MateriaEntity materia = materiaRepository.save(
                new MateriaEntity("CONC-" + sufijo, "Sistemas Distribuidos", 8, 1)
        );
        Long alumno1Id = alumnoService.crear(
                "M1" + sufijo, "Grace Hopper", "grace." + sufijo + "@uni.mx"
        ).id();
        Long alumno2Id = alumnoService.crear(
                "M2" + sufijo, "Alan Turing", "alan." + sufijo + "@uni.mx"
        ).id();

        // 2. Coordinar dos hilos para invocar simultáneamente el proxy real de InscripcionService
        CountDownLatch listos = new CountDownLatch(2);
        CountDownLatch disparo = new CountDownLatch(1);
        var pool = Executors.newFixedThreadPool(2);

        try {
            Callable<Long> tarea1 = () -> {
                listos.countDown();
                disparo.await(5, TimeUnit.SECONDS);
                return inscripcionService.inscribir(alumno1Id, materia.getId());
            };
            Callable<Long> tarea2 = () -> {
                listos.countDown();
                disparo.await(5, TimeUnit.SECONDS);
                return inscripcionService.inscribir(alumno2Id, materia.getId());
            };

            Future<Long> f1 = pool.submit(tarea1);
            Future<Long> f2 = pool.submit(tarea2);

            assertTrue(listos.await(5, TimeUnit.SECONDS));
            disparo.countDown(); // Libera ambos hilos al mismo instante

            List<Long> exitos = new ArrayList<>();
            List<Throwable> fallos = new ArrayList<>();

            for (Future<Long> futuro : List.of(f1, f2)) {
                try {
                    exitos.add(futuro.get(10, TimeUnit.SECONDS));
                } catch (ExecutionException ex) {
                    fallos.add(ex.getCause());
                }
            }

            // 3. Verificar invariante: 1 inscripción persistida y 1 rechazada por SIN_CUPO
            assertEquals(1, exitos.size(), "Debe persistirse exactamente una inscripción");
            assertEquals(1, fallos.size(), "Debe rechazarse exactamente una solicitud");

            OperacionAcademicaException errorNegocio =
                    assertInstanceOf(OperacionAcademicaException.class, fallos.getFirst());
            assertEquals("SIN_CUPO", errorNegocio.getCodigo());
            assertEquals(1L, inscripcionRepository.countByMateriaId(materia.getId()));
        } finally {
            pool.shutdownNow();
        }
    }
}