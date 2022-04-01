package dev.kybu.passbook.async;

public interface ThrowingConsumer<T> {
    void accept(T t) throws Throwable;
}
