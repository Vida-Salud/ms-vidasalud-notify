package cl.duoc.ms_vidasalud_notify.service;

import cl.duoc.ms_vidasalud_notify.dto.DatosAtencion;
import cl.duoc.ms_vidasalud_notify.dto.MensajeComando;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class RecordService {

    private static final Logger log = LoggerFactory.getLogger(RecordService.class);

    private final SimuladorFallas simuladorFallas;

    public RecordService(SimuladorFallas simuladorFallas) {
        this.simuladorFallas = simuladorFallas;
    }

    public void generarComprobante(MensajeComando mensaje) {
        DatosAtencion data = mensaje.data();
        simuladorFallas.verificar(data, "Generador de PDF");
        log.info("Comprobante PDF generado atención {} para {} (estado {}) [eventId={}]", data.atencionId(), data.paciente(), data.estado(), mensaje.eventId());
    }
}
