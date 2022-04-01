package dev.kybu.passbook;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import dev.kybu.passbook.commands.PassbookCommand;
import dev.kybu.passbook.service.AnyDatabaseService;
import dev.kybu.passbook.service.PassbookService;
import dev.kybu.passbook.service.impl.DummyDatabaseServiceProvider;
import dev.kybu.passbook.service.impl.PassbookServiceProvider;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Field;
import java.util.concurrent.Executors;

public final class Passbook extends JavaPlugin {

    public static final ListeningExecutorService EXECUTOR_SERVICE = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(2));

    @Override
    public void onEnable() {
        registerAllServices();
        registerAllCommands();
    }

    @Override
    public void onDisable() {

    }

    private void registerAllServices() {
        Bukkit.getServicesManager().register(PassbookService.class, new PassbookServiceProvider(), this, ServicePriority.High);
        Bukkit.getServicesManager().register(AnyDatabaseService.class, new DummyDatabaseServiceProvider(), this, ServicePriority.High);
    }

    private void registerAllCommands() {
        try {
            registerCommand(new PassbookCommand());
        } catch(final Throwable throwable) {
            throwable.printStackTrace();
        }
    }

    private void registerCommand(final Command command) throws Exception {
        final Field fieldCommandMap = Bukkit.getServer().getClass().getDeclaredField("commandMap");
        fieldCommandMap.setAccessible(true);
        final CommandMap commandMap = (CommandMap) fieldCommandMap.get(Bukkit.getServer());
        commandMap.register(command.getName(), command);
    }
}
