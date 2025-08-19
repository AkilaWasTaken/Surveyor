package network.akila.surveyor;

import fr.mrmicky.fastinv.FastInvManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class Surveyor extends JavaPlugin {

    @Override
    public void onEnable() {
        FastInvManager.register(this);

    }

    @Override
    public void onDisable() {
    }
}
