package cl.duoc.ms_vidasalud_notify;

import cl.duoc.ms_vidasalud_notify.config.EmailProperties;
import cl.duoc.ms_vidasalud_notify.config.IdempotenciaProperties;
import cl.duoc.ms_vidasalud_notify.config.ReintentosProperties;
import cl.duoc.ms_vidasalud_notify.config.SimulacionProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({ReintentosProperties.class, IdempotenciaProperties.class,
        SimulacionProperties.class, EmailProperties.class})
public class MsVidasaludNotifyApplication {

	public static void main(String[] args) {
		SpringApplication.run(MsVidasaludNotifyApplication.class, args);
	}

}
