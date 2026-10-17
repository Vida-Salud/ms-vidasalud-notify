package cl.duoc.ms_vidasalud_notify.consumer.support;

import cl.duoc.ms_vidasalud_notify.config.IdempotenciaProperties;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;


@Component
public class RegistroIdempotencia {

    private final Map<String, Boolean> procesados;

    public RegistroIdempotencia(IdempotenciaProperties properties) {
        int maxRegistros = properties.maxRegistros();
        this.procesados = new LinkedHashMap<>(16, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, Boolean> eldest) {
                return size() > maxRegistros;
            }
        };
    }

    public synchronized boolean yaProcesado(String cola, String eventId) {
        return procesados.get(clave(cola, eventId)) != null;
    }

    public synchronized void marcarProcesado(String cola, String eventId) {
        procesados.put(clave(cola, eventId), Boolean.TRUE);
    }

    private static String clave(String cola, String eventId) {
        return cola + ":" + eventId;
    }
}
