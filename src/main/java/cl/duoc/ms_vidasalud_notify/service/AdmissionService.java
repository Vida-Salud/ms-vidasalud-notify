package cl.duoc.ms_vidasalud_notify.service;

import cl.duoc.ms_vidasalud_notify.dto.DatosAtencion;
import cl.duoc.ms_vidasalud_notify.dto.MensajeComando;
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

    public void emitirTicket(MensajeComando mensaje) {
        DatosAtencion data = mensaje.data();
        simuladorFallas.verificar(data, "Sistema de admisión");
        log.info("Ticket de admisión emitido box {} para {} (atención {}) [eventId={}]", data.box(), data.paciente(), data.atencionId(), mensaje.eventId());
    }
}
