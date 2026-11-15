package cl.duoc.ms_vidasalud_notify.service;

import cl.duoc.ms_vidasalud_notify.dto.DatosAtencion;

public interface InyectorFallas {
    void verificar(DatosAtencion data, String sistema);
}
