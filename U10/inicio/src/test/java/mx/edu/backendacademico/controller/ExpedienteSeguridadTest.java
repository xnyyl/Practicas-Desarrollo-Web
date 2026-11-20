package mx.edu.backendacademico.controller;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import mx.edu.backendacademico.config.SecurityConfig;
import mx.edu.backendacademico.domain.Alumno;
import mx.edu.backendacademico.domain.EstatusAlumno;
import mx.edu.backendacademico.service.AlumnoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ExpedienteController.class)
@Import(SecurityConfig.class)
class ExpedienteSeguridadTest {

    @Autowired MockMvc mvc;
    @MockitoBean AlumnoService service;

    @Test
    void propietarioConsultaSuPropioExpedienteViaMe() throws Exception {
        when(service.buscarPorMatricula("A001"))
                .thenReturn(new Alumno(1L, "A001", "Ada Lovelace", "ada@u.mx", EstatusAlumno.ACTIVO));

        // Aunque el cliente mande ?matricula=A002, el servidor ignora el parámetro y usa el principal (A001)
        mvc.perform(get("/api/v1/alumnos/me?matricula=A002").with(httpBasic("A001", "alumno123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.matricula").value("A001"));
    }

    @Test
    void propietarioConsultaSuExpedientePorMatriculaExplicita() throws Exception {
        when(service.buscarPorMatricula("A001"))
                .thenReturn(new Alumno(1L, "A001", "Ada Lovelace", "ada@u.mx", EstatusAlumno.ACTIVO));

        mvc.perform(get("/api/v1/alumnos/A001/expediente").with(httpBasic("A001", "alumno123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.matricula").value("A001"));
    }

    @Test
    void usuarioAjenoRecibe403AlIntentarLeerOtroExpediente() throws Exception {
        mvc.perform(get("/api/v1/alumnos/A001/expediente").with(httpBasic("A002", "alumno123")))
                .andExpect(status().isForbidden());

        verifyNoInteractions(service);
    }

    @Test
    void administradorPuedeConsultarExpedienteDeCualquierAlumno() throws Exception {
        when(service.buscarPorMatricula("A001"))
                .thenReturn(new Alumno(1L, "A001", "Ada Lovelace", "ada@u.mx", EstatusAlumno.ACTIVO));

        mvc.perform(get("/api/v1/alumnos/A001/expediente").with(httpBasic("admin", "admin123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.matricula").value("A001"));
    }
}