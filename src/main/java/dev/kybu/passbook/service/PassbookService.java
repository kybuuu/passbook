package dev.kybu.passbook.service;

import com.google.common.util.concurrent.ListenableFuture;
import dev.kybu.passbook.data.PassbookData;

import java.util.UUID;

public interface PassbookService {

    ListenableFuture<Boolean> hasPassbook(final UUID uuid);

    ListenableFuture<PassbookData> getPassbook(final UUID uuid);

    void savePassbook(final PassbookData passbookData);

    ListenableFuture<Void> pushPassbook(final PassbookData passbookData);

    ListenableFuture<Void> deletePassbook(final UUID uuid);

}
