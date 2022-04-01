package dev.kybu.passbook.async;

public interface ThrowingFunction<T, R> {
    R apply(final T input) throws Throwable;
}
