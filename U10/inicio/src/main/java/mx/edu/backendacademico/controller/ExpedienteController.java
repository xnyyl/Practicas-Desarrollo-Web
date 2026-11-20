package mx.edu.backendacademico.controller;

import mx.edu.backendacademico.dto.AlumnoMapper;
import mx.edu.backendacademico.dto.AlumnoResponse;
import mx.edu.backendacademico.service.AlumnoService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/alumnos")
public class ExpedienteController {

    private final AlumnoService alumnoService;

    public ExpedienteController(AlumnoService alumnoService) {
        this.alumnoService = alumnoService;
    }

    // 1. Consulta directa de "mi expediente" usando el principal autenticado
    @GetMapping("/me")
    public AlumnoResponse miExpediente(Authentication authentication) {
        String matriculaAutenticada = authentication.getName();
        return AlumnoMapper.toResponse(
                alumnoService.buscarPorMatricula(matriculaAutenticada)
        );
    }

    // 2. Consulta por matrícula: exige ser propietario del expediente o tener rol ADMIN
    @GetMapping("/{matricula}/expediente")
    public AlumnoResponse expedientePorMatricula(
            @PathVariable String matricula,
            Authentication authentication) {

        boolean esAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));

        boolean esPropietario = authentication.getName().equalsIgnoreCase(matricula);

        if (!esAdmin && !esPropietario) {
            throw new AccessDeniedException("No tienes permiso para consultar el expediente de otra matrícula.");
        }

        return AlumnoMapper.toResponse(
                alumnoService.buscarPorMatricula(matricula)
        );
    }
}