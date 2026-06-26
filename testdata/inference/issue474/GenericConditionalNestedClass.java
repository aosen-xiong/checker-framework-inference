import java.util.ArrayList;
import java.util.List;

class GenericConditionalNestedClass<ID, T> {
    static class Box<ID, T> {}

    static class Worker<ID, T> {
        List<Box<ID, T>> f(List<Box<ID, T>> xs) {
            return xs == null ? new ArrayList<>() : xs;
        }
    }
}
