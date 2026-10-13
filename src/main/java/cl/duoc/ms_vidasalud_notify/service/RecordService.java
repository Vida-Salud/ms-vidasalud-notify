package cl.duoc.ms_vidasalud_notify.service;

import cl.duoc.ms_vidasalud_notify.dto.DatosAtencion;
import cl.duoc.ms_vidasalud_notify.dto.MensajeComando;
import cl.duoc.ms_vidasalud_notify.exception.ErrorPermanenteException;
import cl.duoc.ms_vidasalud_notify.exception.ErrorTemporalException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class RecordService {

    private static final Logger log = LoggerFactory.getLogger(RecordService.class);

    public void generarComprobante(MensajeComando mensaje) {
        DatosAtencion data = mensaje.data();
        if ("FALLA_TEMPORAL".equals(data.paciente())) {
            throw new ErrorTemporalException("Generador de PDF no disponible");
        }
        if ("FALLA_PERMANENTE".equals(data.paciente())) {
            throw new ErrorPermanenteException("Datos del paciente inválidos para generar comprobante");
        }
        log.info("Comprobante PDF generado atención {} para {} (estado {}) [eventId={}]", data.atencionId(), data.paciente(), data.estado(), mensaje.eventId());
    }
}
