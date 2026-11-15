package cl.duoc.ms_vidasalud_notify;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noFields;

class ArquitecturaTest {

    private static final JavaClasses CLASES = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("cl.duoc");

    @Test
    void sinInyeccionPorCampo() {
        noFields().should().beAnnotatedWith(Autowired.class)
                .because("las dependencias se inyectan por constructor")
                .check(CLASES);
    }
}
