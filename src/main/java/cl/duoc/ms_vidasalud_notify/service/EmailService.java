package cl.duoc.ms_vidasalud_notify.service;

import cl.duoc.ms_vidasalud_notify.config.EmailProperties;
import cl.duoc.ms_vidasalud_notify.dto.DatosAtencion;
import cl.duoc.ms_vidasalud_notify.dto.MensajeComando;
import cl.duoc.ms_vidasalud_notify.exception.ErrorPermanenteException;
import cl.duoc.ms_vidasalud_notify.exception.ErrorTemporalException;
import java.text.Normalizer;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;
import org.springframework.mail.MailParseException;
import org.springframework.mail.MailPreparationException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Envía los emails al paciente por SMTP (en desarrollo, a Mailpit) según el {@code type} del mensaje.
 * <ul>
 *   <li><b>Errores permanentes</b> ({@link ErrorPermanenteException}, van directo a la DLQ): tipo de email
 *       no soportado, faltan datos para armar el texto, el paciente no permite armar un destinatario o el
 *       mensaje de email es inválido ({@link MailParseException}, {@link MailPreparationException}).</li>
 *   <li><b>Errores temporales</b> ({@link ErrorTemporalException}, se reintentan con backoff): cualquier otra
 *       {@link MailException}, por ejemplo el servidor SMTP caído o sin responder dentro del timeout.</li>
 * </ul>
 */
@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    public static final String TYPE_CONFIRMADA = "email.confirmada";
    public static final String TYPE_LLAMADO_BOX = "email.llamado-box";
    public static final String TYPE_CIERRE = "email.cierre";
    public static final String TYPE_CANCELADA = "email.cancelada";
    public static final String TYPE_ATENCION_CANCELADA = "atencion.cancelada";

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final String BOX_POR_DEFECTO = "recepción";

    private final SimuladorFallas simuladorFallas;
    private final JavaMailSender mailSender;
    private final EmailProperties emailProperties;

    public EmailService(SimuladorFallas simuladorFallas, JavaMailSender mailSender,
                        EmailProperties emailProperties) {
        this.simuladorFallas = simuladorFallas;
        this.mailSender = mailSender;
        this.emailProperties = emailProperties;
    }

    public void enviar(MensajeComando mensaje) {
        DatosAtencion data = mensaje.data();
        String type = mensaje.type();
        simuladorFallas.verificar(data, "Servidor de email");
        String texto = armarTexto(type, data);
        String asunto = armarAsunto(type);
        String destinatario = armarDestinatario(data.paciente());

        SimpleMailMessage email = new SimpleMailMessage();
        email.setFrom(emailProperties.remitente());
        email.setTo(destinatario);
        email.setSubject(asunto);
        email.setText(texto);
        try {
            mailSender.send(email);
        } catch (MailParseException | MailPreparationException e) {
            throw new ErrorPermanenteException("Email inválido para " + destinatario, e);
        } catch (MailException e) {
            Throwable causa = e.getMostSpecificCause();
            throw new ErrorTemporalException("Servidor de email no disponible: "
                    + causa.getClass().getSimpleName() + ": " + causa.getMessage(), e);
        }

        log.info("Email enviado a {} <{}>: \"{}\" [type={}, eventId={}]",
                data.paciente(), destinatario, texto, type, mensaje.eventId());
    }

    private static String armarTexto(String type, DatosAtencion data) {
        return switch (type) {
            case TYPE_CONFIRMADA -> "Tu hora de " + servicio(type, data) + " del " + fecha(type, data)
                    + " está CONFIRMADA.";
            case TYPE_LLAMADO_BOX -> data.box() == null || data.box().isBlank()
                    ? "Te esperamos en " + BOX_POR_DEFECTO + "."
                    : "Te esperamos en el box " + data.box() + ".";
            case TYPE_CIERRE -> "Gracias por tu visita. Tu comprobante de atención ya fue generado.";
            case TYPE_CANCELADA, TYPE_ATENCION_CANCELADA -> "Tu hora de " + servicio(type, data) + " del " + fecha(type, data)
                    + " fue CANCELADA.";
            default -> throw new ErrorPermanenteException("Tipo de email no soportado: " + type);
        };
    }

    private static String armarAsunto(String type) {
        return switch (type) {
            case TYPE_CONFIRMADA -> "VidaSalud: hora confirmada";
            case TYPE_LLAMADO_BOX -> "VidaSalud: te estamos esperando";
            case TYPE_CIERRE -> "VidaSalud: comprobante de tu atención";
            case TYPE_CANCELADA, TYPE_ATENCION_CANCELADA -> "VidaSalud: hora cancelada";
            default -> throw new ErrorPermanenteException("Tipo de email no soportado: " + type);
        };
    }

    private String armarDestinatario(String paciente) {
        if (paciente == null || paciente.isBlank()) {
            throw new ErrorPermanenteException("Falta paciente para armar el destinatario del email");
        }
        String usuario = Normalizer.normalize(paciente, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", ".")
                .replaceAll("^\\.+|\\.+$", "");
        if (usuario.isEmpty()) {
            throw new ErrorPermanenteException("No se puede armar un email para el paciente: " + paciente);
        }
        return usuario + "@" + emailProperties.dominioDestinatario();
    }

    private static String servicio(String type, DatosAtencion data) {
        if (data.servicio() == null || data.servicio().isBlank()) {
            throw new ErrorPermanenteException("Falta servicio para el email " + type);
        }
        return data.servicio();
    }

    private static String fecha(String type, DatosAtencion data) {
        if (data.fechaHora() == null) {
            throw new ErrorPermanenteException("Falta fechaHora para el email " + type);
        }
        return data.fechaHora().format(FORMATO_FECHA);
    }
}
