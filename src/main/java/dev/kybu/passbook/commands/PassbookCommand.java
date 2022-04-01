package dev.kybu.passbook.commands;

import com.google.common.util.concurrent.Futures;
import dev.kybu.passbook.Passbook;
import dev.kybu.passbook.async.FutureCallbackAdapter;
import dev.kybu.passbook.data.PassbookData;
import dev.kybu.passbook.service.PassbookService;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.text.SimpleDateFormat;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public class PassbookCommand extends Command {

    private static final String PREFIX = "§7[§6Sparbuch§7] ";
    private static final SimpleDateFormat SIMPLE_DATE_FORMAT = new SimpleDateFormat("dd.MM.yyyy HH:mm");

    private final PassbookService passbookService;

    public PassbookCommand() {
        super("sparbuch");
        this.passbookService = Bukkit.getServicesManager().load(PassbookService.class);
    }

    @Override
    public boolean execute(CommandSender commandSender, String s, String[] args) {
        if(!(commandSender instanceof Player)) {
            return false;
        }

        final Player player = (Player) commandSender;
        if(args.length == 0) {
            showSyntax(player);
            return false;
        }

        switch (args[0]) {
            case "new":
                createPassbook(player, args);
                break;
            case "info":
                showPassbookInfo(player);
                break;
            case "einzahlen":
                increasePassbookValue(player, args);
                break;
            case "auszahlen":
                payOffPassbook(player, args);
                break;
        }

        return false;
    }

    private void showSyntax(final Player player) {
        Futures.addCallback(this.passbookService.getPassbook(player.getUniqueId()), new FutureCallbackAdapter<PassbookData>() {
            @Override
            public void onSuccess(final PassbookData passbookData) {
                if(passbookData == null) {
                    player.sendMessage(PREFIX + "/sparbuch new [Ziel]");
                } else {
                    player.sendMessage(PREFIX + "/sparbuch [info, einzahlen, auszahlen]");
                }
            }
        }, Passbook.EXECUTOR_SERVICE);
    }

    private void createPassbook(final Player player, final String[] args) {
        Futures.addCallback(this.passbookService.hasPassbook(player.getUniqueId()), new FutureCallbackAdapter<Boolean>() {
            @Override
            public void onSuccess(Boolean hasPassbook) {
                if(hasPassbook) {
                    player.sendMessage(PREFIX + "§cDu hast bereits ein Sparbuch");
                    return;
                }

                if(args.length != 2) {
                    player.sendMessage(PREFIX + "/sparbuch new [Ziel]");
                    return;
                }

                try {
                    final int goal = Integer.parseInt(args[1]);
                    final PassbookData passbookData = new PassbookData(player.getUniqueId(), 0, goal,  System.currentTimeMillis());
                    pushPassbook(passbookData, unused -> {
                        player.sendMessage(PREFIX + "§7Du hast erfolgreich ein Sparbuch mit einem Ziel in Höhe von §e" + passbookData.getPassbookGoal() + "$ §7erstellt!");
                    });
                } catch(final Throwable throwable) {
                    player.sendMessage(PREFIX + "§cDas Ziel muss eine Zahl sein");
                }
            }
        }, Passbook.EXECUTOR_SERVICE);
    }

    private void showPassbookInfo(final Player player) {
        Futures.addCallback(this.passbookService.hasPassbook(player.getUniqueId()), new FutureCallbackAdapter<Boolean>() {
            @Override
            public void onSuccess(Boolean hasPassbook) {
                if(!hasPassbook) {
                    player.sendMessage(PREFIX + "§cDu hast kein Sparbuch");
                    return;
                }

                getPassbook(player.getUniqueId(), passbookData -> {
                    player.sendMessage(PREFIX + "7Deine Sparbuch Informationen");
                    player.sendMessage(PREFIX + "§7Derzeitiger Betrag: §b" + passbookData.getPassbookValue());
                    player.sendMessage(PREFIX + "§7Ziel: §b" + passbookData.getPassbookGoal());
                    player.sendMessage(PREFIX + "§7Erstellt am: §b" + SIMPLE_DATE_FORMAT.format(passbookData.getPassbookCreationTimestamp()));
                });
            }
        }, Passbook.EXECUTOR_SERVICE);
    }

    private void increasePassbookValue(final Player player, final String[] args) {
        Futures.addCallback(this.passbookService.hasPassbook(player.getUniqueId()), new FutureCallbackAdapter<Boolean>() {
            @Override
            public void onSuccess(Boolean hasPassbook) {
                if(!hasPassbook) {
                    player.sendMessage(PREFIX + "§cDu hast kein Sparbuch");
                    return;
                }

                if(args.length != 2) {
                    player.sendMessage(PREFIX + "/sparbuch einzahlen [Betrag]");
                    return;
                }

                try {
                    AtomicInteger value = new AtomicInteger(Integer.parseInt(args[1]));
                    if(value.get() <= 0) {
                        player.sendMessage(PREFIX + "§cDu kannst nicht weniger als 1$ einzahlen");
                        return;
                    }
                    // TODO: Einen Check dafür, ob der Spieler über das Bargeld überhaupt verfügt
                    getPassbook(player.getUniqueId(), passbookData -> {
                        if((passbookData.getPassbookValue() + value.get()) > passbookData.getPassbookGoal()) {
                            value.set(passbookData.getPassbookGoal() - passbookData.getPassbookValue());
                        }
                        passbookData.setPassbookValue(passbookData.getPassbookValue() + value.get());
                        // TODO: Dem Spieler den Betrag value vom Bargeld abziehen
                        pushPassbook(passbookData, unused -> {
                            player.sendMessage(PREFIX + "Du hast §e" + value.get() + "$ §7in dein Sparbuch eingezahlt");
                            if(passbookData.getPassbookValue() > passbookData.getPassbookGoal()) {
                                player.sendMessage(PREFIX + "§aDu hast dein Sparbuch Ziel erreicht!");
                                player.playSound(player.getEyeLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1, 1);
                            }
                        });
                    });
                } catch(final Throwable throwable) {
                    player.sendMessage(PREFIX + "§cDer Betrag muss eine Zahl sein");
                }
            }
        }, Passbook.EXECUTOR_SERVICE);
    }

    private void payOffPassbook(final Player player, final String[] args) {
        Futures.addCallback(this.passbookService.hasPassbook(player.getUniqueId()), new FutureCallbackAdapter<Boolean>() {
            @Override
            public void onSuccess(Boolean hasPassbook) {
                if(!hasPassbook) {
                    player.sendMessage(PREFIX + "§cDu hast kein Sparbuch");
                    return;
                }

                getPassbook(player.getUniqueId(), passbookData -> {
                    if(passbookData.getPassbookValue() < passbookData.getPassbookGoal()) {
                        player.sendMessage(PREFIX + "§cDu hast dein Sparbuch Ziel noch nicht erreicht!");
                        return;
                    }

                    if(args.length < 2 || !args[1].equalsIgnoreCase("confirm")) {
                        player.sendMessage(PREFIX + "Möchtest du dein Sparbuch wirklich auszahlen?");
                        player.sendMessage(PREFIX + "Bestätige mit /sparbuch auszahlen confirm");
                        return;
                    }

                    if(!hasWeekPassedSinceCreation(passbookData)) {
                        player.sendMessage(PREFIX + "Es ist seit der Erstellung noch keine Woche vergangen");
                        player.sendMessage(PREFIX + "Du kannst dein Sparbuch am §e" + convertTime(passbookData.getPassbookCreationTimestamp() + TimeUnit.DAYS.toMillis(7)) + " §7auszahlen");
                        return;
                    }

                    deletePassbook(player.getUniqueId(), unused -> {
                        player.sendMessage(PREFIX + "§7Du hast dein Sparbuch ausgezahlt!");
                        player.sendMessage(PREFIX + "§a+ " + passbookData.getPassbookValue() + "$");
                        // TODO: Dem Spieler den Betrag auf die Hand auszahlen
                    });

                });
            }
        }, Passbook.EXECUTOR_SERVICE);
    }

    private boolean hasWeekPassedSinceCreation(final PassbookData passbookData) {
        final long timePassed = System.currentTimeMillis() - passbookData.getPassbookCreationTimestamp();
        return timePassed >= TimeUnit.DAYS.toMillis(7);
    }

    private String convertTime(final long time) {
        return SIMPLE_DATE_FORMAT.format(time);
    }

    private void pushPassbook(final PassbookData passbookData, final Consumer<Void> onSuccess) {
        Futures.addCallback(this.passbookService.pushPassbook(passbookData), new FutureCallbackAdapter<Void>() {
            @Override
            public void onSuccess(Void unused) {
                onSuccess.accept(unused);
            }
        }, Passbook.EXECUTOR_SERVICE);
    }

    private void deletePassbook(final UUID owner, final Consumer<Void> onSuccess) {
        Futures.addCallback(this.passbookService.deletePassbook(owner), new FutureCallbackAdapter<Void>() {
            @Override
            public void onSuccess(Void unused) {
                onSuccess.accept(unused);
            }
        }, Passbook.EXECUTOR_SERVICE);
    }

    private void getPassbook(final UUID uuid, final Consumer<PassbookData> onSuccess) {
        Futures.addCallback(this.passbookService.getPassbook(uuid), new FutureCallbackAdapter<PassbookData>() {
            @Override
            public void onSuccess(PassbookData passbookData) {
                onSuccess.accept(passbookData);
            }
        }, Passbook.EXECUTOR_SERVICE);
    }
}
