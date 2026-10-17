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
 * Procesa los mensajes de las colas de trabajo de notify con una política común de confirmación y errores.
 * Los consumidores solo le pasan el mensaje, el canal, su cola y la acción de negocio.
 * <p>
 * El listener trabaja con <b>ACK manual</b> y <b>prefetch 1</b> ({@code spring.rabbitmq.listener.simple}):
 * cada consumidor tiene un solo mensaje sin confirmar a la vez y decide explícitamente su destino.
 * <ol>
 *   <li><b>Mensaje inválido</b> (JSON roto o vacío, o falta {@code eventId}, {@code type} o {@code data}):
 *       NACK sin requeue &rarr; el broker lo envía al DLX y queda en la DLQ de la cola. No se reintenta.</li>
 *   <li><b>Duplicado</b>: si {@link RegistroIdempotencia} ya tiene {@code cola:eventId}, se hace ACK y el
 *       mensaje se ignora sin ejecutar la acción.</li>
 *   <li><b>Éxito</b>: la acción termina sin errores &rarr; se marca {@code cola:eventId} como procesado y
 *       después se hace ACK. Si el ACK falla, el reenvío de RabbitMQ se detecta como duplicado.</li>
 *   <li><b>{@link ErrorPermanenteException}</b>: NACK sin requeue inmediato &rarr; DLQ, sin reintentar.</li>
 *   <li><b>{@link ErrorTemporalException}</b> (o cualquier otra excepción inesperada): se reintenta en el mismo
 *       hilo con backoff exponencial según {@code notify.reintentos} (intervalo inicial, multiplicador y
 *       máximo de reintentos; por defecto 1 s, 2 s y 4 s). Agotados los reintentos: NACK sin requeue &rarr; DLQ.
 *       Las excepciones inesperadas se registran con stack trace.</li>
 *   <li><b>Interrupción del hilo</b> durante la espera entre reintentos (por ejemplo, al apagar la app):
 *       NACK con requeue &rarr; el mensaje vuelve a su cola, para no mandarlo a la DLQ por una causa
 *       ajena al mensaje.</li>
 * </ol>
 * Las DLQ no se consumen: el envío a la DLQ se registra en el log en el momento del NACK, indicando
 * la cola de origen, el DLX, la DLQ de destino, el {@code eventId}, el motivo y el número de intentos.
 * Los mensajes que terminan en la DLQ no se marcan como procesados.
 */
@Component
public class ProcesadorMensajes {

    private static final Logger log = LoggerFactory.getLogger(ProcesadorMensajes.class);
    private static final String EVENT_ID_DESCONOCIDO = "desconocido";
    private static final String LOG_DLQ =
            "[-> DLX] {}. NACK enviado -> DLX={} -> DLQ={} [cola={}, eventId={}, motivo={}, intentos={}]";

    private final JsonMapper jsonMapper;
    private final ReintentosProperties reintentos;
    private final RegistroIdempotencia registro;

    public ProcesadorMensajes(JsonMapper jsonMapper, ReintentosProperties reintentos,
                              RegistroIdempotencia registro) {
        this.jsonMapper = jsonMapper;
        this.reintentos = reintentos;
        this.registro = registro;
    }

    /**
     * Procesa un mensaje y lo confirma (ACK) o rechaza (NACK) según la política descrita en la clase.
     *
     * @param message mensaje crudo recibido de RabbitMQ (body JSON con el envelope {@link MensajeComando})
     * @param channel canal del listener, usado para el ACK/NACK manual
     * @param cola    cola de origen (constante de {@link RabbitMQConfig}); define la DLQ y la clave de idempotencia
     * @param accion  acción de negocio del servicio que corresponde a la cola
     * @throws IOException si falla el ACK/NACK sobre el canal
     */
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

        if (registro.yaProcesado(cola, mensaje.eventId())) {
            channel.basicAck(deliveryTag, false);
            log.info("Duplicado ignorado [cola={}, eventId={}]", cola, mensaje.eventId());
            return;
        }

        int intento = 1;
        long espera = reintentos.intervaloInicialMs();
        while (true) {
            try {
                accion.accept(mensaje);
                registro.marcarProcesado(cola, mensaje.eventId());
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
