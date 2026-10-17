package cl.duoc.ms_vidasalud_notify.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("notify.email")
public record EmailProperties(String remitente, String dominioDestinatario) {
}
