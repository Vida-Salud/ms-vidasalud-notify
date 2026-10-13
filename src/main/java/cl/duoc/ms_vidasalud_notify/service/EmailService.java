package cl.duoc.ms_vidasalud_notify.service;

import cl.duoc.ms_vidasalud_notify.dto.DatosAtencion;
import cl.duoc.ms_vidasalud_notify.dto.MensajeComando;
import cl.duoc.ms_vidasalud_notify.exception.ErrorPermanenteException;
import cl.duoc.ms_vidasalud_notify.exception.ErrorTemporalException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    public void enviar(MensajeComando mensaje) {
        DatosAtencion data = mensaje.data();
        if ("FALLA_TEMPORAL".equals(data.paciente())) {
            throw new ErrorTemporalException("Servidor de email no disponible");
        }
        if ("FALLA_PERMANENTE".equals(data.paciente())) {
            throw new ErrorPermanenteException("Datos del paciente inválidos para enviar email");
        }
        log.info("Email enviado a {} (servicio {}, fecha {}) [eventId={}]", data.paciente(), data.servicio(), data.fechaHora(), mensaje.eventId());
    }
}
