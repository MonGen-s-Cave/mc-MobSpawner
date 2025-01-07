package hu.kxtsoo.mobspawner.manager;

import hu.kxtsoo.mobspawner.MobSpawner;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;

public final class SchedulerManager {

    private static boolean isFolia;

    static {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            isFolia = true;

        } catch (final ClassNotFoundException e) {
            isFolia = false;
        }
    }

    public static void run(Runnable runnable) {
        if (isFolia)
            Bukkit.getGlobalRegionScheduler().execute(MobSpawner.getInstance(), runnable);

        else
            Bukkit.getScheduler().runTask(MobSpawner.getInstance(), runnable);
    }

    public static void nrun(Runnable runnable) {
        if (isFolia)
            Bukkit.getAsyncScheduler().runNow(MobSpawner.getInstance(), task -> {
                Bukkit.getScheduler().runTask(MobSpawner.getInstance(), runnable);
            });
        else
            Bukkit.getScheduler().runTask(MobSpawner.getInstance(), runnable);
    }

    public static void runAsync(Runnable runnable) {
        if (isFolia)
            Bukkit.getGlobalRegionScheduler().execute(MobSpawner.getInstance(), runnable);
        else
            Bukkit.getScheduler().runTaskAsynchronously(MobSpawner.getInstance(), runnable);
    }

    public static Task runLater(Runnable runnable, long delayTicks) {
        if (isFolia)
            return new Task(Bukkit.getGlobalRegionScheduler()
                    .runDelayed(MobSpawner.getInstance(), t -> runnable.run(), delayTicks));

        else
            return new Task(Bukkit.getScheduler().runTaskLater(MobSpawner.getInstance(), runnable, delayTicks));
    }

    public static Task runAsyncLater(Runnable runnable, long delayTicks) {
        if (isFolia)
            return new Task(Bukkit.getGlobalRegionScheduler()
                    .runDelayed(MobSpawner.getInstance(), t -> runnable.run(), delayTicks));

        else
            return new Task(Bukkit.getScheduler().runTaskLaterAsynchronously(MobSpawner.getInstance(), runnable, delayTicks));
    }

    public static Task runTimer(Runnable runnable, long delayTicks, long periodTicks) {
        if (isFolia)
            return new Task(Bukkit.getGlobalRegionScheduler()
                    .runAtFixedRate(MobSpawner.getInstance(), t -> runnable.run(), delayTicks < 1 ? 1 : delayTicks, periodTicks));

        else
            return new Task(Bukkit.getScheduler().runTaskTimer(MobSpawner.getInstance(), runnable, delayTicks, periodTicks));
    }

    public static Task runAsyncTimer(Runnable runnable, long delayTicks, long periodTicks) {
        if (isFolia)
            return new Task(Bukkit.getGlobalRegionScheduler()
                    .runAtFixedRate(MobSpawner.getInstance(), t -> runnable.run(), delayTicks < 1 ? 1 : delayTicks, periodTicks));

        else
            return new Task(Bukkit.getScheduler().runTaskTimerAsynchronously(MobSpawner.getInstance(), runnable, delayTicks, periodTicks));
    }

    public static boolean isFolia() {
        return isFolia;
    }

    public static class Task {
        private ScheduledTask foliaTask;
        private BukkitTask bukkitTask;

        Task(ScheduledTask foliaTask) {
            this.foliaTask = foliaTask;
        }

        Task(BukkitTask bukkitTask) {
            this.bukkitTask = bukkitTask;
        }

        public void cancel() {
            if (foliaTask != null) {
                foliaTask.cancel();
            } else if (bukkitTask != null) {
                bukkitTask.cancel();
            }
        }
    }
}