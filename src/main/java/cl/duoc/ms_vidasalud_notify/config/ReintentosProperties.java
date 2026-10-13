package cl.duoc.ms_vidasalud_notify.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("notify.reintentos")
public record ReintentosProperties(int maxIntentos, long intervaloInicialMs, double multiplicador) {
}
