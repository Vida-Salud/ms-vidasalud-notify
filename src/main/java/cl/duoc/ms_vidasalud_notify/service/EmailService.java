package cl.duoc.ms_vidasalud_notify.service;

import cl.duoc.ms_vidasalud_notify.dto.DatosAtencion;
import cl.duoc.ms_vidasalud_notify.dto.MensajeComando;
import cl.duoc.ms_vidasalud_notify.exception.ErrorPermanenteException;
import java.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    public static final String TYPE_CONFIRMADA = "email.confirmada";
    public static final String TYPE_LLAMADO_BOX = "email.llamado-box";
    public static final String TYPE_CIERRE = "email.cierre";
    public static final String TYPE_CANCELADA = "email.cancelada";

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final String BOX_POR_DEFECTO = "recepción";

    private final SimuladorFallas simuladorFallas;

    public EmailService(SimuladorFallas simuladorFallas) {
        this.simuladorFallas = simuladorFallas;
    }

    public void enviar(MensajeComando mensaje) {
        DatosAtencion data = mensaje.data();
        simuladorFallas.verificar(data, "Servidor de email");
        String texto = armarTexto(mensaje.type(), data);
        log.info("Email enviado a {}: \"{}\" [type={}, eventId={}]",
                data.paciente(), texto, mensaje.type(), mensaje.eventId());
    }

    private static String armarTexto(String type, DatosAtencion data) {
        return switch (type) {
            case TYPE_CONFIRMADA -> "Tu hora de " + servicio(type, data) + " del " + fecha(type, data)
                    + " está CONFIRMADA.";
            case TYPE_LLAMADO_BOX -> data.box() == null || data.box().isBlank()
                    ? "Te esperamos en " + BOX_POR_DEFECTO + "."
                    : "Te esperamos en el box " + data.box() + ".";
            case TYPE_CIERRE -> "Gracias por tu visita. Adjuntamos el comprobante de tu atención.";
            case TYPE_CANCELADA -> "Tu hora de " + servicio(type, data) + " del " + fecha(type, data)
                    + " fue CANCELADA.";
            default -> throw new ErrorPermanenteException("Tipo de email no soportado: " + type);
        };
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
