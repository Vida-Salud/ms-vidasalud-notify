package cl.duoc.ms_vidasalud_notify.service;

import cl.duoc.ms_vidasalud_notify.dto.DatosAtencion;
import cl.duoc.ms_vidasalud_notify.dto.MensajeComando;
import cl.duoc.ms_vidasalud_notify.exception.ErrorPermanenteException;
import cl.duoc.ms_vidasalud_notify.exception.ErrorTemporalException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class AdmissionService {

    private static final Logger log = LoggerFactory.getLogger(AdmissionService.class);

    public void emitirTicket(MensajeComando mensaje) {
        DatosAtencion data = mensaje.data();
        if ("FALLA_TEMPORAL".equals(data.paciente())) {
            throw new ErrorTemporalException("Sistema de admisión no disponible");
        }
        if ("FALLA_PERMANENTE".equals(data.paciente())) {
            throw new ErrorPermanenteException("Datos del paciente inválidos para emitir ticket");
        }
        log.info("Ticket de admisión emitido box {} para {} (atención {}) [eventId={}]", data.box(), data.paciente(), data.atencionId(), mensaje.eventId());
    }
}
