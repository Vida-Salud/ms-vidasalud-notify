package cl.duoc.ms_vidasalud_notify.service;

import cl.duoc.ms_vidasalud_notify.dto.DatosAtencion;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "notify.simulacion.habilitada", havingValue = "false", matchIfMissing = true)
public class SinFallas implements InyectorFallas {

    @Override
    public void verificar(DatosAtencion data, String sistema) {
        // Fuera de la demo no se inyectan fallas.
    }
}
