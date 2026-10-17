package cl.duoc.ms_vidasalud_notify.service;

import cl.duoc.ms_vidasalud_notify.config.SimulacionProperties;
import cl.duoc.ms_vidasalud_notify.dto.DatosAtencion;
import cl.duoc.ms_vidasalud_notify.exception.ErrorPermanenteException;
import cl.duoc.ms_vidasalud_notify.exception.ErrorTemporalException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Herramienta de prueba para provocar fallas a voluntad y demostrar los reintentos y el envío a la DLQ.
 * <p>
 * Si {@code notify.simulacion.habilitada} es {@code true}, el paciente {@code FALLA_TEMPORAL} genera una
 * {@link ErrorTemporalException} (se reintenta y termina en la DLQ) y el paciente {@code FALLA_PERMANENTE}
 * una {@link ErrorPermanenteException} (va directo a la DLQ). En producción debe ir deshabilitada
 * ({@code NOTIFY_SIMULACION=false}).
 */
@Component
public class SimuladorFallas {

    private static final Logger log = LoggerFactory.getLogger(SimuladorFallas.class);
    private static final String PACIENTE_FALLA_TEMPORAL = "FALLA_TEMPORAL";
    private static final String PACIENTE_FALLA_PERMANENTE = "FALLA_PERMANENTE";

    private final boolean habilitada;

    public SimuladorFallas(SimulacionProperties simulacion) {
        this.habilitada = simulacion.habilitada();
        if (habilitada) {
            log.warn("Simulación de fallas HABILITADA: los pacientes {} y {} generan errores de prueba",
                    PACIENTE_FALLA_TEMPORAL, PACIENTE_FALLA_PERMANENTE);
        }
    }

    public void verificar(DatosAtencion data, String sistema) {
        if (!habilitada) {
            return;
        }
        if (PACIENTE_FALLA_TEMPORAL.equals(data.paciente())) {
            throw new ErrorTemporalException(sistema + " no disponible (simulado)");
        }
        if (PACIENTE_FALLA_PERMANENTE.equals(data.paciente())) {
            throw new ErrorPermanenteException("Datos inválidos para " + sistema + " (simulado)");
        }
    }
}
