package hudson.plugins.octopusdeploy.utils;

import com.google.common.base.Supplier;
import static java.util.Objects.requireNonNull;

public final class Lazy<T> {
    private volatile T value;

    public T getOrCompute(Supplier<T> supplier) {
        final T result = value;
        return result == null ? maybeCompute(supplier) : result;
    }

    private synchronized T maybeCompute(Supplier<T> supplier) {
        if (value == null) {
            value = requireNonNull(supplier.get());
        }
        return value;
    }
}
