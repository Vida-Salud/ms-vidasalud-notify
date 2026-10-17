package cl.duoc.ms_vidasalud_notify.service;

import static cl.duoc.ms_vidasalud_notify.config.TiposMensaje.ADMISSION_TICKET;
import static cl.duoc.ms_vidasalud_notify.config.TiposMensaje.ATENCION_CANCELADA;

import cl.duoc.ms_vidasalud_notify.dto.DatosAtencion;
import cl.duoc.ms_vidasalud_notify.dto.MensajeComando;
import cl.duoc.ms_vidasalud_notify.exception.ErrorPermanenteException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class AdmissionService {

    private static final Logger log = LoggerFactory.getLogger(AdmissionService.class);

    private final SimuladorFallas simuladorFallas;

    public AdmissionService(SimuladorFallas simuladorFallas) {
        this.simuladorFallas = simuladorFallas;
    }

    public void procesar(MensajeComando mensaje) {
        DatosAtencion data = mensaje.data();
        simuladorFallas.verificar(data, "Sistema de admisión");
        switch (mensaje.type()) {
            case ADMISSION_TICKET -> log.info("Ticket de admisión emitido box {} para {} (atención {}) [eventId={}]",
                    data.box(), data.paciente(), data.atencionId(), mensaje.eventId());
            case ATENCION_CANCELADA -> liberarCupo(mensaje);
            default -> throw new ErrorPermanenteException("Tipo no soportado en admisión: " + mensaje.type());
        }
    }

    private static void liberarCupo(MensajeComando mensaje) {
        DatosAtencion data = mensaje.data();
        if (data.box() == null || data.box().isBlank()) {
            log.info("Atención {} cancelada sin box asignado, no hay cupo que liberar [eventId={}]",
                    data.atencionId(), mensaje.eventId());
        } else {
            log.info("Cupo del box {} liberado (atención {} cancelada) [eventId={}]",
                    data.box(), data.atencionId(), mensaje.eventId());
        }
    }
}
