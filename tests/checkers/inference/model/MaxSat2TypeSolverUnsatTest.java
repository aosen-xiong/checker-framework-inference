package checkers.inference.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.checkerframework.framework.type.QualifierHierarchy;
import org.checkerframework.javacutil.AnnotationMirrorSet;
import org.junit.Test;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVisitor;

import checkers.inference.InferenceResult;
import checkers.inference.solver.MaxSat2TypeSolver;

public class MaxSat2TypeSolverUnsatTest {
    private static final AnnotationMirror TOP = new FakeAnnotationMirror("test.Top");
    private static final AnnotationMirror BOTTOM = new FakeAnnotationMirror("test.Bottom");

    @Test
    public void reportsUnsatisfiableConstraints() {
        TestVariableSlot variable = new TestVariableSlot(1);
        ConstantSlot top = new ConstantSlot(2, TOP);
        ConstantSlot bottom = new ConstantSlot(3, BOTTOM);

        Constraint equalTop =
                EqualityConstraint.create(variable, top, AnnotationLocation.MISSING_LOCATION);
        Constraint equalBottom =
                EqualityConstraint.create(variable, bottom, AnnotationLocation.MISSING_LOCATION);
        List<Constraint> constraints = Arrays.asList(equalTop, equalBottom);

        System.out.println("=== MaxSat2 unsat explanation example ===");
        System.out.println("input constraints:");
        for (Constraint constraint : constraints) {
            System.out.println("  " + describe(constraint));
        }

        InferenceResult result =
                new MaxSat2TypeSolver()
                        .solve(
                                Collections.emptyMap(),
                                Collections.singletonList(variable),
                                constraints,
                                new FakeQualifierHierarchy(),
                                (ProcessingEnvironment) null);

        assertFalse(result.hasSolution());
        Collection<Constraint> unsatConstraints = result.getUnsatisfiableConstraints();
        System.out.println("solver has solution: " + result.hasSolution());
        System.out.println("unsat explanation:");
        for (Constraint constraint : unsatConstraints) {
            System.out.println("  " + describe(constraint));
        }
        assertEquals(2, unsatConstraints.size());
        assertTrue(unsatConstraints.contains(equalTop));
        assertTrue(unsatConstraints.contains(equalBottom));
    }

    private static String describe(Constraint constraint) {
        if (constraint instanceof EqualityConstraint) {
            EqualityConstraint equalityConstraint = (EqualityConstraint) constraint;
            return describe(equalityConstraint.getFirst())
                    + " == "
                    + describe(equalityConstraint.getSecond());
        }
        return constraint.toString();
    }

    private static String describe(Slot slot) {
        if (slot instanceof ConstantSlot) {
            return ((ConstantSlot) slot).getValue().toString();
        }
        return "slot#" + slot.getId();
    }

    private static final class TestVariableSlot extends VariableSlot {
        private TestVariableSlot(int id) {
            super(id, AnnotationLocation.MISSING_LOCATION);
        }

        @Override
        public Kind getKind() {
            return Kind.VARIABLE;
        }

        @Override
        public boolean isInsertable() {
            return true;
        }

        @Override
        public <S, T> S serialize(checkers.inference.model.Serializer<S, T> serializer) {
            return null;
        }
    }

    private static final class FakeQualifierHierarchy extends QualifierHierarchy {
        private FakeQualifierHierarchy() {
            super(null);
        }

        @Override
        public AnnotationMirrorSet getTopAnnotations() {
            return new AnnotationMirrorSet(TOP);
        }

        @Override
        public AnnotationMirror getTopAnnotation(AnnotationMirror start) {
            return TOP;
        }

        @Override
        public AnnotationMirrorSet getBottomAnnotations() {
            return new AnnotationMirrorSet(BOTTOM);
        }

        @Override
        public AnnotationMirror getBottomAnnotation(AnnotationMirror start) {
            return BOTTOM;
        }

        @Override
        public AnnotationMirror getPolymorphicAnnotation(AnnotationMirror start) {
            return null;
        }

        @Override
        public boolean isPolymorphicQualifier(AnnotationMirror qualifier) {
            return false;
        }

        @Override
        protected boolean isSubtypeQualifiers(
                AnnotationMirror subQualifier, AnnotationMirror superQualifier) {
            return subQualifier == BOTTOM || superQualifier == TOP || subQualifier == superQualifier;
        }

        @Override
        protected AnnotationMirror leastUpperBoundQualifiers(
                AnnotationMirror qualifier1, AnnotationMirror qualifier2) {
            return qualifier1 == qualifier2 ? qualifier1 : TOP;
        }

        @Override
        protected AnnotationMirror greatestLowerBoundQualifiers(
                AnnotationMirror qualifier1, AnnotationMirror qualifier2) {
            return qualifier1 == qualifier2 ? qualifier1 : BOTTOM;
        }
    }

    private static final class FakeAnnotationMirror implements AnnotationMirror {
        private final String name;
        private final DeclaredType type;

        private FakeAnnotationMirror(String name) {
            this.name = name;
            this.type = new FakeDeclaredType(name);
        }

        @Override
        public DeclaredType getAnnotationType() {
            return type;
        }

        @Override
        public Map<? extends ExecutableElement, ? extends AnnotationValue> getElementValues() {
            return Collections.emptyMap();
        }

        @Override
        public String toString() {
            return name;
        }
    }

    private static final class FakeDeclaredType implements DeclaredType {
        private final String name;
        private final TypeElement element;

        private FakeDeclaredType(String name) {
            this.name = name;
            this.element = fakeTypeElement(name);
        }

        @Override
        public TypeMirror getEnclosingType() {
            return null;
        }

        @Override
        public Element asElement() {
            return element;
        }

        @Override
        public List<? extends TypeMirror> getTypeArguments() {
            return Collections.emptyList();
        }

        @Override
        public TypeKind getKind() {
            return TypeKind.DECLARED;
        }

        @Override
        public <R, P> R accept(TypeVisitor<R, P> visitor, P parameter) {
            return visitor.visitDeclared(this, parameter);
        }

        @Override
        public <A extends Annotation> A getAnnotation(Class<A> annotationType) {
            return null;
        }

        @Override
        public <A extends Annotation> A[] getAnnotationsByType(Class<A> annotationType) {
            return null;
        }

        @Override
        public List<? extends AnnotationMirror> getAnnotationMirrors() {
            return Collections.emptyList();
        }

        @Override
        public String toString() {
            return name;
        }
    }

    private static TypeElement fakeTypeElement(String name) {
        InvocationHandler handler =
                new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) {
                        if (method.getName().equals("getQualifiedName")
                                || method.getName().equals("getSimpleName")) {
                            return new FakeName(name);
                        }
                        if (method.getName().equals("toString")) {
                            return name;
                        }
                        if (method.getName().equals("getEnclosedElements")) {
                            return Collections.emptyList();
                        }
                        return null;
                    }
                };
        return (TypeElement)
                Proxy.newProxyInstance(
                        MaxSat2TypeSolverUnsatTest.class.getClassLoader(),
                        new Class<?>[] {TypeElement.class},
                        handler);
    }

    private static final class FakeName implements Name {
        private final String value;

        private FakeName(String value) {
            this.value = value;
        }

        @Override
        public boolean contentEquals(CharSequence cs) {
            return value.contentEquals(cs);
        }

        @Override
        public int length() {
            return value.length();
        }

        @Override
        public char charAt(int index) {
            return value.charAt(index);
        }

        @Override
        public CharSequence subSequence(int start, int end) {
            return value.subSequence(start, end);
        }

        @Override
        public String toString() {
            return value;
        }
    }
}
