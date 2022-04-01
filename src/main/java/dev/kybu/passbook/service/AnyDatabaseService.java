package dev.kybu.passbook.service;

import com.google.common.util.concurrent.ListenableFuture;
import dev.kybu.passbook.async.ThrowingConsumer;
import dev.kybu.passbook.async.ThrowingFunction;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.function.Consumer;
import java.util.function.Function;

public interface AnyDatabaseService {

    ListenableFuture<Void> executeCommand(final String command, ThrowingConsumer<PreparedStatement> preCommandBindingConsumer);

    <T> ListenableFuture<T> executeCommand(final String command, final ThrowingConsumer<PreparedStatement> preCommandBindingConsumer, final ThrowingFunction<ResultSet, T> outputFunction);

}
