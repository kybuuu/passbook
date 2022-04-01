package dev.kybu.passbook.data;

import java.util.UUID;

public class PassbookData {

    private UUID passbookOwner;
    private int passbookValue;
    private int passbookGoal;
    private long passbookCreationTimestamp;

    /*=---------------------------------------------------------=*/

    public PassbookData(final UUID passbookOwner, final int passbookValue, final int passbookGoal, final long passbookCreationTimestamp) {
        this.passbookOwner = passbookOwner;
        this.passbookValue = passbookValue;
        this.passbookGoal = passbookGoal;
        this.passbookCreationTimestamp = passbookCreationTimestamp;
    }

    /*=---------------------------------------------------------=*/

    public UUID getPassbookOwner() {
        return passbookOwner;
    }

    public int getPassbookValue() {
        return passbookValue;
    }

    public int getPassbookGoal() {
        return passbookGoal;
    }

    public long getPassbookCreationTimestamp() {
        return passbookCreationTimestamp;
    }

    /*=---------------------------------------------------------=*/

    public void setPassbookOwner(UUID passbookOwner) {
        this.passbookOwner = passbookOwner;
    }

    public void setPassbookValue(int passbookValue) {
        this.passbookValue = passbookValue;
    }

    public void setPassbookGoal(int passbookGoal) {
        this.passbookGoal = passbookGoal;
    }

    public void setPassbookCreationTimestamp(long passbookCreationTimestamp) {
        this.passbookCreationTimestamp = passbookCreationTimestamp;
    }
}
