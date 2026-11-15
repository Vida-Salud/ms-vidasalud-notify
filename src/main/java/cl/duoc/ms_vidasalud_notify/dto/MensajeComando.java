package cl.duoc.ms_vidasalud_notify.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;

@JsonIgnoreProperties(ignoreUnknown = true)
public record MensajeComando(
        Integer version,
        String eventId,
        String type,
        Instant timestamp,
        String correlationId,
        String traceId,
        DatosAtencion data) {
}
