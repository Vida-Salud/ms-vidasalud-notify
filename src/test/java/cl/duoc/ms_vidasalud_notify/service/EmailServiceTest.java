package cl.duoc.ms_vidasalud_notify.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import cl.duoc.ms_vidasalud_notify.config.EmailProperties;
import cl.duoc.ms_vidasalud_notify.config.SimulacionProperties;
import cl.duoc.ms_vidasalud_notify.config.TiposMensaje;
import cl.duoc.ms_vidasalud_notify.dto.DatosAtencion;
import cl.duoc.ms_vidasalud_notify.dto.MensajeComando;
import cl.duoc.ms_vidasalud_notify.exception.ErrorTemporalException;
import java.time.Instant;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

class EmailServiceTest {

    private JavaMailSender mailSender;
    private EmailService emailService;

    @BeforeEach
    void setUp() {
        mailSender = mock(JavaMailSender.class);
        emailService = new EmailService(
                new SimuladorFallas(new SimulacionProperties(false)),
                mailSender,
                new EmailProperties("notificaciones@vidasalud.cl", "vidasalud.cl"));
    }

    @Test
    void confirmadaEnviaEmailAlPacienteConAsuntoYTexto() {
        emailService.enviar(mensaje(TiposMensaje.EMAIL_CONFIRMADA, "pablo"));

        SimpleMailMessage enviado = capturarEmail();
        assertThat(enviado.getFrom()).isEqualTo("notificaciones@vidasalud.cl");
        assertThat(enviado.getTo()).containsExactly("pablo@vidasalud.cl");
        assertThat(enviado.getSubject()).isEqualTo("VidaSalud: hora confirmada");
        assertThat(enviado.getText()).contains("CONFIRMADA");
    }

    @Test
    void normalizaNombreConTildesYEspacios() {
        emailService.enviar(mensaje(TiposMensaje.EMAIL_CONFIRMADA, "José Pérez"));

        assertThat(capturarEmail().getTo()).containsExactly("jose.perez@vidasalud.cl");
    }

    @Test
    void servidorSmtpCaidoEsErrorTemporal() {
        doThrow(new MailSendException("Connection refused")).when(mailSender).send(any(SimpleMailMessage.class));

        assertThatThrownBy(() -> emailService.enviar(mensaje(TiposMensaje.EMAIL_CONFIRMADA, "pablo")))
                .isInstanceOf(ErrorTemporalException.class)
                .hasMessageContaining("Servidor de email no disponible");
    }

    private SimpleMailMessage capturarEmail() {
        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        return captor.getValue();
    }

    private static MensajeComando mensaje(String type, String paciente) {
        DatosAtencion data = new DatosAtencion(1L, paciente, "Medicina General", "B3",
                LocalDateTime.of(2026, 10, 20, 9, 30), "AGENDADA");
        return new MensajeComando("evt-1", type, Instant.now(), "corr-1", data);
    }
}
