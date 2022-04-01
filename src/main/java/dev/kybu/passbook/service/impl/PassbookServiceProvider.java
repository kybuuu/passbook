package dev.kybu.passbook.service.impl;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalCause;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListenableFutureTask;
import dev.kybu.passbook.data.PassbookData;
import dev.kybu.passbook.service.AnyDatabaseService;
import dev.kybu.passbook.service.PassbookService;
import org.bukkit.Bukkit;

import java.sql.ResultSet;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * TABLE STRUCTURE Passbook
 * OWNER (Primary Key), PASSBOOK_BALANCE, PASSBOOK_GOAL, CREATION_TIMESTAMP
 */
public class PassbookServiceProvider implements PassbookService {

    private final AnyDatabaseService databaseService;
    private final Cache<UUID, PassbookData> passbookCache;

    public PassbookServiceProvider() {
        this.databaseService = Bukkit.getServicesManager().load(AnyDatabaseService.class);
        this.passbookCache = CacheBuilder.newBuilder().removalListener(removalNotification -> {
            if(removalNotification.getCause() == RemovalCause.EXPIRED) {
                final PassbookData passbookData = (PassbookData) removalNotification.getValue();
                if(passbookData == null) {
                    System.out.println("Removal Listener reported null value");
                    return;
                }
                pushPassbook((PassbookData) removalNotification.getValue());
            }
        }).expireAfterWrite(5, TimeUnit.MINUTES).build();
        try {
            this.databaseService.executeCommand("CREATE TABLE IF NOT EXISTS Passbook(OWNER VARCHAR(36), PASSBOOK_BALANCE INT, PASSBOOK_GOAL INT, CREATION_TIMESTAMP BIGINT, PRIMARY KEY (OWNER))", preparedStatement -> {}).get();
        } catch(final Throwable throwable) {
            throwable.printStackTrace();
        }
    }

    @Override
    public ListenableFuture<Boolean> hasPassbook(UUID uuid) {
        if(this.passbookCache.getIfPresent(uuid) != null) {
            return Futures.immediateFuture(true);
        }

        return ListenableFutureTask.create(() -> this.databaseService.executeCommand("SELECT OWNER FROM Passbook WHERE OWNER=?", preparedStatement -> {
            preparedStatement.setString(1, uuid.toString());
        }, ResultSet::next).get());
    }

    @Override
    public ListenableFuture<PassbookData> getPassbook(UUID uuid) {
        if(this.passbookCache.getIfPresent(uuid) != null) {
            return Futures.immediateFuture(passbookCache.getIfPresent(uuid));
        }

        return ListenableFutureTask.create(() -> {
            PassbookData passbookData = this.databaseService.executeCommand("SELECT OWNER, PASSBOOK_BALANCE, PASSBOOK_GOAL, CREATION_TIMESTAMP FROM Passbook WHERE UUID=?", preparedStatement -> {
                preparedStatement.setString(1, uuid.toString());
            }, resultSet -> {
                if(resultSet.next()) {
                    return new PassbookData(UUID.fromString(resultSet.getString(1)), resultSet.getInt(2), resultSet.getInt(3), resultSet.getLong(4));
                }
                return null;
            }).get();
            passbookCache.put(uuid, passbookData);
            return passbookData;
        });
    }

    @Override
    public void savePassbook(final PassbookData passbookData) {
        if(this.passbookCache.getIfPresent(passbookData.getPassbookOwner()) != null) {
            this.passbookCache.invalidate(passbookData.getPassbookOwner());
        }
        this.passbookCache.put(passbookData.getPassbookOwner(), passbookData);
    }

    @Override
    public ListenableFuture<Void> pushPassbook(PassbookData passbookData) {
        return ListenableFutureTask.create(() -> {
            try {
                savePassbook(passbookData);
                boolean hasPassbook = hasPassbook(passbookData.getPassbookOwner()).get();
                if(hasPassbook) {
                    PassbookServiceProvider.this.databaseService.executeCommand("UPDATE Passbook SET PASSBOOK_BALANCE=? WHERE OWNER=?", preparedStatement -> {
                        preparedStatement.setInt(1, passbookData.getPassbookValue());
                        preparedStatement.setString(2, passbookData.getPassbookOwner().toString());
                    }).get();
                }  else {
                    PassbookServiceProvider.this.databaseService.executeCommand("INSERT INTO Passbook VALUES(?, ?, ?, ?)", preparedStatement -> {
                        preparedStatement.setString(1, passbookData.getPassbookOwner().toString());
                        preparedStatement.setInt(2, passbookData.getPassbookValue());
                        preparedStatement.setInt(3, passbookData.getPassbookGoal());
                        preparedStatement.setLong(4, passbookData.getPassbookCreationTimestamp());
                    }).get();
                }
            } catch(final Throwable throwable) {
                throwable.printStackTrace();
            }
            return null;
        });
    }

    @Override
    public ListenableFuture<Void> deletePassbook(UUID uuid) {
        if(this.passbookCache.getIfPresent(uuid) != null) {
            this.passbookCache.invalidate(uuid);
        }
        return this.databaseService.executeCommand("DELETE FROM Passbook WHERE OWNER=?", preparedStatement -> {
            preparedStatement.setString(1, uuid.toString());
        });
    }

}
