package cl.duoc.ms_vidasalud_notify;

import cl.duoc.ms_vidasalud_notify.config.ReintentosProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(ReintentosProperties.class)
public class MsVidasaludNotifyApplication {

	public static void main(String[] args) {
		SpringApplication.run(MsVidasaludNotifyApplication.class, args);
	}

}
