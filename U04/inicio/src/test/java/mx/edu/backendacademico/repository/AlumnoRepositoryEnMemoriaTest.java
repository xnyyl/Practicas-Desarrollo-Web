package mx.edu.backendacademico.repository;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import mx.edu.backendacademico.domain.Alumno;
import mx.edu.backendacademico.domain.EstatusAlumno;
import mx.edu.backendacademico.exception.MatriculaDuplicadaException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class AlumnoRepositoryEnMemoriaTest {

    @Test
    void guardarConcurrenteMantieneUnicidadDeMatricula() throws InterruptedException {
        AlumnoRepositoryEnMemoria repository = new AlumnoRepositoryEnMemoria();

        // startLatch asegura que ambos hilos arranquen casi simultáneamente.
        CountDownLatch startLatch = new CountDownLatch(1);
        // doneLatch nos permite esperar a que ambas tareas terminen sin usar tiempos fijos.
        CountDownLatch doneLatch = new CountDownLatch(2);

        AtomicInteger registrosExitosos = new AtomicInteger(0);
        AtomicInteger conflictosDetectados = new AtomicInteger(0);

        Runnable tarea = () -> {
            try {
                startLatch.await();
                Alumno alumno = new Alumno(null, "A01", "Nombre", "correo@test.com", EstatusAlumno.ACTIVO);
                repository.guardar(alumno);
                registrosExitosos.incrementAndGet();
            } catch (MatriculaDuplicadaException e) {
                conflictosDetectados.incrementAndGet();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                doneLatch.countDown();
            }
        };

        new Thread(tarea).start();
        new Thread(tarea).start();

        // Dispara la ejecución concurrente
        startLatch.countDown();
        // Bloquea el hilo principal hasta que el contador de doneLatch llegue a 0
        doneLatch.await();

        // Evidencia: Un hilo logró guardar, el otro fue rechazado.
        Assertions.assertEquals(1, registrosExitosos.get(), "Debe haber exactamente un registro exitoso");
        Assertions.assertEquals(1, conflictosDetectados.get(), "Debe haber exactamente un rechazo por duplicidad");
        Assertions.assertEquals(1, repository.buscarTodos().size(), "El repositorio debe contener solo un alumno");
    }
}
