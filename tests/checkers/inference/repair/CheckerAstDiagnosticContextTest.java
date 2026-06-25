package checkers.inference.repair;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import checkers.inference.test.CheckerDiagnosticCapture;

import org.junit.Test;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class CheckerAstDiagnosticContextTest {
    private static final Class<?> NNINF_CHECKER = nninf.NninfChecker.class;
    private static final List<String> NNINF_OPTIONS =
            Arrays.asList("-Anomsgtext", "-d", "tests/build/outputdir");

    @Test
    public void mapsNullableDereferenceDiagnosticToReceiverAst() {
        CheckerDiagnosticCapture.Result capture =
                CheckerDiagnosticCapture.run(
                        NNINF_CHECKER,
                        new File("testdata/repair/NullableReceiverDerefRepair.java"),
                        NNINF_OPTIONS);
        List<RepairDiagnostic> diagnostics = RepairDiagnosticAdapter.fromCaptureResult(capture);

        assertEquals(1, diagnostics.size());
        RepairDiagnostic diagnostic = diagnostics.get(0);
        assertEquals("dereference.of.nullable", diagnostic.getKey());
        assertTrue(diagnostic.getColumnNumber() > 0);
        assertTrue(diagnostic.getPosition() >= 0);

        CheckerAstDiagnosticContext context = CheckerAstDiagnosticContext.from(diagnostic);
        Optional<NullableDereferenceAstContext> nullableDereference =
                context.nullableDereference();

        assertTrue(nullableDereference.isPresent());
        assertEquals("builder", nullableDereference.get().getReceiverSource());
        assertEquals("builder.add(name, value)", nullableDereference.get().getDereferenceSource());

        Optional<NullableFieldAstContext> nullableField =
                context.nullableFieldForReceiver(nullableDereference.get());
        assertTrue(nullableField.isPresent());
        assertEquals("builder", nullableField.get().getFieldName());
        assertEquals("Builder", nullableField.get().getDeclaredTypeSource());
    }
}
