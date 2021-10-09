# What is this
Some sort of magic that allows hosting a steam server and a non steam one at the same time. The main advantage of this is that non steam players can be identified easily as their "ip" is an integer linked with their steam account rather than a normal ipv4/6 so steam players can verify themselves (steam players are unlikely to pay $5 to grief).

# How do I make this work?
Put the jar in the server's mods folder and place a file named `steam_appid.txt` and inside it type `1127400`. This will ensure that steam launches mindustry correctly when started. Keep in mind this requires you to be logged into a steam account that owns the game. One account cannot host multiple games at once without problems. This also means that you cant really use that steam account for anything else while the server is running (you can't even connect to it either).
