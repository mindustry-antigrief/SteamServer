package steamServer;

import arc.*;
import arc.util.*;
import com.codedisaster.steamworks.*;
import mindustry.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.mod.*;
import mindustry.net.*;

import java.lang.reflect.*;

public class SteamServer extends Plugin {
    boolean steamOnly = Core.settings.getBool("steamserver-steamonly", false);;

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

        Events.on(EventType.PlayerJoin.class, event -> {
            Player p = event.player;
            long steamID64 = SVars.net.steamConnections.get(Strings.parseInt(p.ip())).sid.handle();

            Events.fire(new SteamVerificationEvent(p, steamID64));
            Log.infoTag("VERIFICATION", "PLAYER:" + p + " | STEAMID64: " + steamID64);

            if (!Strings.canParseInt(p.ip())) { // Non-Steam connection
                if (steamOnly) {
                    Call.connect(p.con, "steam:" + SteamID.getNativeHandle(SVars.net.currentLobby), 0);
                    Call.announce(p.con, "[scarlet]This server is in steam only mode, you can only connect by playing on steam!");
                    p.remove();
                }
            }
        });

        Log.info("Initialized SteamServer!");
    }

    @Override
    public void registerServerCommands(CommandHandler handler){ // FINISHME: This is just stupid
        Log.info("Run the steamonly command to toggle whether or not non-steam players can join.");
        handler.register("steamonly", "Toggles whether the server allows non-steam players", args -> {
            steamOnly ^= true;
            Core.settings.put("steamserver-steamonly", steamOnly);
            Log.info("SteamOnly has been set to: " + steamOnly);
        });
    }

    public static class SteamVerificationEvent {
        public final Player player;
        /** Null when not a steam connection */
        public final long steamID64;

        public SteamVerificationEvent(Player player, long steamID64) {
            this.player = player;
            this.steamID64 = steamID64;
        }
    }
}
