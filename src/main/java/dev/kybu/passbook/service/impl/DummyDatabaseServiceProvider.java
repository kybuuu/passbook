package dev.kybu.passbook.service.impl;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListenableFutureTask;
import dev.kybu.passbook.async.ThrowingConsumer;
import dev.kybu.passbook.async.ThrowingFunction;
import dev.kybu.passbook.service.AnyDatabaseService;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class DummyDatabaseServiceProvider implements AnyDatabaseService {

    private final Connection connection;

    public DummyDatabaseServiceProvider() {
        this.connection = null;
    }

    @Override
    public ListenableFuture<Void> executeCommand(final String command, final ThrowingConsumer<PreparedStatement> preCommandBindingConsumer) {
        return ListenableFutureTask.create(() -> {
            try(final PreparedStatement preparedStatement = connection.prepareStatement(command)) {
                preCommandBindingConsumer.accept(preparedStatement);
                preparedStatement.executeQuery();
            } catch(final Throwable exception) {
                System.err.println("Du benutzt einen Dummy, weshalb der SQL-Command nicht wirklich ausgeführt wurde");
            }
            return null;
        });
    }

    @Override
    public <T> ListenableFuture<T> executeCommand(final String command, final ThrowingConsumer<PreparedStatement> preCommandBindingConsumer, final ThrowingFunction<ResultSet, T> outputFunction) {
        return ListenableFutureTask.create(() -> {
            try(final PreparedStatement preparedStatement = connection.prepareStatement(command)) {
                preCommandBindingConsumer.accept(preparedStatement);
                try(final ResultSet resultSet = preparedStatement.executeQuery()) {
                    return outputFunction.apply(resultSet);
                }
            } catch(final Throwable exception) {
                System.err.println("Du benutzt einen Dummy, weshalb der SQL-Command nicht wirklich ausgeführt wurde");
            }
            return null;
        });
    }

}
