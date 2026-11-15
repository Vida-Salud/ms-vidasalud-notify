package cl.duoc.ms_vidasalud_notify.consumer.support;

import cl.duoc.ms_vidasalud_notify.config.IdempotenciaProperties;
import cl.duoc.ms_vidasalud_notify.config.RabbitMQConfig;
import cl.duoc.ms_vidasalud_notify.config.ReintentosProperties;
import cl.duoc.ms_vidasalud_notify.exception.ErrorPermanenteException;
import cl.duoc.ms_vidasalud_notify.exception.ErrorTemporalException;
import com.rabbitmq.client.Channel;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class ProcesadorMensajesTest {

    private final Channel channel = mock(Channel.class);
    private final ProcesadorMensajes procesador = new ProcesadorMensajes(
            JsonMapper.builder().build(),
            new ReintentosProperties(3, 1, 1.0),
            new RegistroIdempotencia(new IdempotenciaProperties(100)));

    @Test
    void exitoHaceAckYUnDuplicadoNoRepiteLaAccion() throws Exception {
        AtomicInteger ejecuciones = new AtomicInteger();
        procesador.procesar(mensaje(1L, 1), channel, RabbitMQConfig.EMAIL_QUEUE, m -> ejecuciones.incrementAndGet());
        procesador.procesar(mensaje(1L, 1), channel, RabbitMQConfig.EMAIL_QUEUE, m -> ejecuciones.incrementAndGet());

        assertThat(ejecuciones).hasValue(1);
        verify(channel, times(2)).basicAck(1L, false);
    }

    @Test
    void mensajeSinVersionSeProcesaComoAnteriorAlContrato() throws Exception {
        AtomicInteger ejecuciones = new AtomicInteger();
        String json = """
                {"eventId":"e-9","type":"email.confirmada","timestamp":"2026-11-15T12:00:00Z",
                 "data":{"atencionId":1,"paciente":"Ana","servicio":"Dental","fechaHora":"2026-11-20T10:00:00"}}
                """;
        procesador.procesar(crudo(9L, json), channel, RabbitMQConfig.EMAIL_QUEUE, m -> ejecuciones.incrementAndGet());

        assertThat(ejecuciones).hasValue(1);
        verify(channel).basicAck(9L, false);
    }

    @Test
    void versionDesconocidaVaDirectoALaDlq() throws Exception {
        procesador.procesar(mensaje(2L, 2), channel, RabbitMQConfig.EMAIL_QUEUE, m -> fail("no debe ejecutarse"));
        verify(channel).basicNack(2L, false, false);
    }

    @Test
    void errorPermanenteVaDirectoALaDlq() throws Exception {
        procesador.procesar(mensaje(3L, 1), channel, RabbitMQConfig.EMAIL_QUEUE,
                m -> { throw new ErrorPermanenteException("dato inválido"); });
        verify(channel).basicNack(3L, false, false);
        verify(channel, never()).basicAck(anyLong(), anyBoolean());
    }

    @Test
    void errorTemporalReintentaYTerminaEnLaDlq() throws Exception {
        AtomicInteger intentos = new AtomicInteger();
        procesador.procesar(mensaje(4L, 1), channel, RabbitMQConfig.EMAIL_QUEUE,
                m -> { intentos.incrementAndGet(); throw new ErrorTemporalException("SMTP caído"); });
        assertThat(intentos).hasValue(4); // 1 intento + 3 reintentos
        verify(channel).basicNack(4L, false, false);
    }

    @Test
    void jsonInvalidoVaALaDlq() throws Exception {
        procesador.procesar(crudo(5L, "{no es json"), channel, RabbitMQConfig.EMAIL_QUEUE,
                m -> fail("no debe ejecutarse"));
        verify(channel).basicNack(5L, false, false);
    }

    @Test
    void mensajeSinEventIdVaALaDlq() throws Exception {
        String json = """
                {"version":1,"type":"email.confirmada",
                 "data":{"atencionId":1,"paciente":"Ana","servicio":"Dental","fechaHora":"2026-11-20T10:00:00"}}
                """;
        procesador.procesar(crudo(6L, json), channel, RabbitMQConfig.EMAIL_QUEUE, m -> fail("no debe ejecutarse"));
        verify(channel).basicNack(6L, false, false);
    }

    private static Message mensaje(long deliveryTag, int version) {
        String json = """
                {"version":%d,"eventId":"e-%d","type":"email.confirmada",
                 "timestamp":"2026-11-15T12:00:00Z",
                 "data":{"atencionId":1,"paciente":"Ana","servicio":"Dental","fechaHora":"2026-11-20T10:00:00"}}
                """.formatted(version, deliveryTag);
        return crudo(deliveryTag, json);
    }

    private static Message crudo(long deliveryTag, String json) {
        MessageProperties props = new MessageProperties();
        props.setDeliveryTag(deliveryTag);
        return new Message(json.getBytes(StandardCharsets.UTF_8), props);
    }
}
