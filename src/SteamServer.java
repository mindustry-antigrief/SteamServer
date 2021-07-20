import arc.ApplicationListener;
import arc.Core;
import arc.Events;
import arc.util.Log;
import arc.util.async.Threads;
import com.codedisaster.steamworks.SteamAPI;
import mindustry.Vars;
import mindustry.game.EventType;
import mindustry.mod.Plugin;
import mindustry.net.Net;

import java.lang.reflect.Field;

public class SteamServer extends Plugin{
    @Override
    public void init() {
        try {
            SteamAPI.loadLibraries();

            if (!SteamAPI.init()) {
                Log.err("Steam client not running.");
            } else {
                initSteam();
                Vars.steam = true;
            }

            if (SteamAPI.restartAppIfNecessary(SVars.steamID)) {
                System.exit(0);
            }
        }catch (NullPointerException ignored) {
            Log.info("Running in offline mode.");
        }catch (Throwable e) {
            Log.err("Failed to load Steam native libraries.");
        }

        // Steam callbacks
        Core.app.addListener (new ApplicationListener() {
            @Override
            public void update() {
                if(SteamAPI.isSteamRunning()) {
                    SteamAPI.runCallbacks();
                }
            }
        });
    }

    void initSteam() throws NoSuchFieldException, IllegalAccessException {
        // Reflection magic to use steam net rather than non steam
        Field ohno = Net.class.getDeclaredField("provider");
        ohno.setAccessible(true);
        ohno.set(Vars.net, SVars.net = new SNet((Net.NetProvider) ohno.get(Vars.net)));

        boolean[] isShutdown = {false};

        Events.on(EventType.DisposeEvent.class, event -> {
            SVars.net.closeServer();
            isShutdown[0] = true;
        });

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (!isShutdown[0]) {
                SVars.net.closeServer();
            }
        }));
    }
}
