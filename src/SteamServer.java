import arc.*;
import arc.util.*;
import arc.util.io.*;
import com.codedisaster.steamworks.*;
import mindustry.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.mod.*;
import mindustry.net.*;

import java.lang.reflect.*;

public class SteamServer extends Plugin {
    boolean steamOnly = getConfig().exists() && getConfig().reads().bool();

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

        Events.on(EventType.ConnectPacketEvent.class, event -> {
            NetConnection con = event.connection;

            if (!Strings.canParseInt(con.address)) { // Non-Steam connection
                if (steamOnly) {
                    Time.run(60, () -> {
                        con.player.remove();
                        Call.connect(con, "steam:" + SteamID.getNativeHandle(SVars.net.currentLobby), 0);
                        Time.run(60, () -> con.kick("This server is in steam only mode, you can only connect by playing on steam!", 0));
                    });
                }
            }
        });
    }

    @Override
    public void registerServerCommands(CommandHandler handler){
        Log.info("Run the steamonly command to toggle whether or not non-steam players can join.");
        handler.register("steamonly", "Toggles whether the server allows non-steam players", args -> {
            steamOnly ^= true;
            Writes out = getConfig().writes();
            out.bool(steamOnly);
            out.close();
            Log.info("SteamOnly has been set to: " + steamOnly);
        });
    }
}
