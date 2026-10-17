package cl.duoc.ms_vidasalud_notify.config;

/**
 * Valores del campo {@code type} del envelope ({@code MensajeComando}); cada servicio decide qué hacer según este
 * valor. ms-vidasalud-appointments usa como {@code type} la misma routing key con la que publica en
 * {@code cmd.direct} o {@code cmd.topic}; en {@code cmd.fanout}, que ignora la routing key, envía
 * {@link #ATENCION_CANCELADA}.
 */
public final class TiposMensaje {

    public static final String EMAIL_CONFIRMADA = "email.confirmada";
    public static final String EMAIL_LLAMADO_BOX = "email.llamado-box";
    public static final String EMAIL_CIERRE = "email.cierre";
    public static final String EMAIL_CANCELADA = "email.cancelada";

    public static final String ADMISSION_TICKET = "admission.ticket";

    public static final String ATENCION_CANCELADA = "atencion.cancelada";

    private TiposMensaje() {
    }
}
