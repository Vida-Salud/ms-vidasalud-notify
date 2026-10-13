package cl.duoc.ms_vidasalud_notify.dto;

import java.time.Instant;

public record MensajeComando(
        String eventId,
        String type,
        Instant timestamp,
        String correlationId,
        DatosAtencion data) {
}
