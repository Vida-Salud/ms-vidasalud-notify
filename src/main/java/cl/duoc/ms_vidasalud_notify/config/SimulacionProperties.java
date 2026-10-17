package cl.duoc.ms_vidasalud_notify.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("notify.simulacion")
public record SimulacionProperties(boolean habilitada) {
}
