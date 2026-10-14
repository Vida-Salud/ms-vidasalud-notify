package cl.duoc.ms_vidasalud_notify.consumer.support;

import cl.duoc.ms_vidasalud_notify.config.RabbitMQConfig;
import cl.duoc.ms_vidasalud_notify.config.ReintentosProperties;
import cl.duoc.ms_vidasalud_notify.dto.MensajeComando;
import cl.duoc.ms_vidasalud_notify.exception.ErrorPermanenteException;
import cl.duoc.ms_vidasalud_notify.exception.ErrorTemporalException;
import com.rabbitmq.client.Channel;
import java.io.IOException;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/**
 * Procesa los mensajes de las colas de trabajo con ACK manual y aplica una política de errores común.
 * <ul>
 *   <li><b>Error permanente</b> ({@link ErrorPermanenteException}): JSON inválido, faltan {@code eventId},
 *       {@code type} o {@code data}, o el servicio lo declara no recuperable. No se reintenta.</li>
 *   <li><b>Error temporal</b> ({@code ErrorTemporalException} o cualquier otra excepción inesperada):
 *       se reintenta hasta {@code notify.reintentos.max-intentos} veces con backoff exponencial
 *       (por defecto 1 s, 2 s y 4 s; hasta 4 ejecuciones en total).</li>
 *   <li><b>ACK</b> ({@code basicAck}): cuando la acción de negocio termina sin errores.</li>
 *   <li><b>NACK sin requeue</b> ({@code basicNack(tag, false, false)}): ante un error permanente o al agotar
 *       los reintentos; el broker lo envía al DLX y queda en la DLQ (TTL 24 h). El log se escribe en ese momento,
 *       ya que las DLQ no se consumen.</li>
 *   <li><b>NACK con requeue</b> ({@code basicNack(tag, false, true)}): si el hilo se interrumpe durante la espera
 *       (por ejemplo, al apagar la aplicación), el mensaje vuelve a la cola para que se procese después
 *       y no termine en la DLQ por una causa ajena al mensaje.</li>
 * </ul>
 */
@Component
public class ProcesadorMensajes {

    private static final Logger log = LoggerFactory.getLogger(ProcesadorMensajes.class);
    private static final String EVENT_ID_DESCONOCIDO = "desconocido";
    private static final String LOG_DLQ =
            "[-> DLX] {}. NACK enviado -> DLX={} -> DLQ={} [cola={}, eventId={}, motivo={}, intentos={}]";

    private final JsonMapper jsonMapper;
    private final ReintentosProperties reintentos;

    public ProcesadorMensajes(JsonMapper jsonMapper, ReintentosProperties reintentos) {
        this.jsonMapper = jsonMapper;
        this.reintentos = reintentos;
    }

    public void procesar(Message message, Channel channel, String cola,
                         Consumer<MensajeComando> accion) throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();

        MensajeComando mensaje = null;
        try {
            mensaje = leer(message);
            validar(mensaje);
        } catch (ErrorPermanenteException e) {
            String eventId = mensaje != null && !vacio(mensaje.eventId()) ? mensaje.eventId() : EVENT_ID_DESCONOCIDO;
            enviarADlq(channel, deliveryTag, cola, eventId, e, 0);
            return;
        }

        int intento = 1;
        long espera = reintentos.intervaloInicialMs();
        while (true) {
            try {
                accion.accept(mensaje);
                channel.basicAck(deliveryTag, false);
                log.info("Mensaje procesado OK [cola={}, eventId={}, type={}, intentos={}]",
                        cola, mensaje.eventId(), mensaje.type(), intento);
                return;
            } catch (ErrorPermanenteException e) {
                enviarADlq(channel, deliveryTag, cola, mensaje.eventId(), e, intento);
                return;
            } catch (RuntimeException e) {
                if (intento > reintentos.maxIntentos()) {
                    enviarADlq(channel, deliveryTag, cola, mensaje.eventId(), e, intento);
                    return;
                }
                log.warn("Fallo temporal [cola={}, eventId={}, intento={}, motivo={}]. Reintento {}/{} en {} ms",
                        cola, mensaje.eventId(), intento, describir(e), intento, reintentos.maxIntentos(), espera);
                if (!esperar(espera)) {
                    channel.basicNack(deliveryTag, false, true);
                    log.warn("Reintentos interrumpidos, mensaje devuelto a la cola [cola={}, eventId={}]",
                            cola, mensaje.eventId());
                    return;
                }
                espera = (long) (espera * reintentos.multiplicador());
                intento++;
            }
        }
    }

    private MensajeComando leer(Message message) {
        try {
            return jsonMapper.readValue(message.getBody(), MensajeComando.class);
        } catch (JacksonException e) {
            throw new ErrorPermanenteException("JSON inválido: " + e.getOriginalMessage(), e);
        }
    }

    private static void validar(MensajeComando mensaje) {
        if (mensaje == null) {
            throw new ErrorPermanenteException("Mensaje vacío");
        }
        if (vacio(mensaje.eventId())) {
            throw new ErrorPermanenteException("Falta eventId");
        }
        if (vacio(mensaje.type())) {
            throw new ErrorPermanenteException("Falta type");
        }
        if (mensaje.data() == null) {
            throw new ErrorPermanenteException("Falta data");
        }
    }

    private void enviarADlq(Channel channel, long deliveryTag, String cola, String eventId,
                            RuntimeException causa, int intentos) throws IOException {
        channel.basicNack(deliveryTag, false, false);
        String dlx = RabbitMQConfig.DEAD_LETTER_EXCHANGE;
        String dlq = RabbitMQConfig.dlqDe(cola);
        String motivo = describir(causa);
        if (causa instanceof ErrorPermanenteException) {
            log.warn(LOG_DLQ, "Error permanente", dlx, dlq, cola, eventId, motivo, intentos);
        } else if (causa instanceof ErrorTemporalException) {
            log.error(LOG_DLQ, "Reintentos agotados", dlx, dlq, cola, eventId, motivo, intentos);
        } else {
            log.error(LOG_DLQ, "Reintentos agotados", dlx, dlq, cola, eventId, motivo, intentos, causa);
        }
    }

    private static boolean esperar(long millis) {
        try {
            Thread.sleep(millis);
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private static boolean vacio(String valor) {
        return valor == null || valor.isBlank();
    }

    private static String describir(Throwable e) {
        return e.getClass().getSimpleName() + ": " + e.getMessage();
    }
}
