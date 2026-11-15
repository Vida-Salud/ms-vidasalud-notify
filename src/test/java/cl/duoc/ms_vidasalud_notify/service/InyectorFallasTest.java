package cl.duoc.ms_vidasalud_notify.service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class InyectorFallasTest {

    private final ApplicationContextRunner contexto = new ApplicationContextRunner()
            .withUserConfiguration(SimuladorFallas.class, SinFallas.class);

    @Test
    void porDefectoNoSeInyectanFallas() {
        contexto.run(ctx -> assertThat(ctx).getBean(InyectorFallas.class).isInstanceOf(SinFallas.class));
    }

    @Test
    void laPropiedadEnciendeLaSimulacion() {
        contexto.withPropertyValues("notify.simulacion.habilitada=true")
                .run(ctx -> assertThat(ctx).getBean(InyectorFallas.class).isInstanceOf(SimuladorFallas.class));
    }
}
