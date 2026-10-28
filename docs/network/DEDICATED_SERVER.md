# Dedicated Java 17 host

Install JDK 17+ and Python 3. Set JAVA_HOME to that JDK (on Windows, use its bin before Oracle's launcher aliases). Build from the repository root:

```text
python tools/build_server.py
java -cp build/dedicated-server.jar hbnet.Setup server-network
```

Setup refuses to overwrite an existing server identity. It generates a persistent PKCS12 certificate/key, protected keystore-password file, salted room verifier and generated room secret. `--manual` reads a 12–128 printable ASCII password from an actual secret console prompt, preserving spaces exactly. No password is passed on argv or printed. Read `server-network/server.fingerprint` and, for generated keys, `server-network/room.secret` privately and deliver out of band. The room secret grants no administrator authority. Protect and back up this directory; Windows owner ACLs/POSIX owner permissions are applied. Keys remain exportable by the OS owner and administrators; no hardware/non-exportability promise is made. Identity/password rotation and recovery need operator review; do not delete identity files as an update procedure.

Start (same Java command works in Windows and Linux):

```text
java -Djava.awt.headless=true -jar build/dedicated-server.jar server-network 27717 3 127.0.0.1 HOUSE
```

`server.cmd` / `sh server.sh` pass the same arguments and supply a 1 GiB heap. Override heap flags with the direct Java command. Default binding is loopback. An explicit bind/listener choice is required for remote reachability; no firewall, UPnP, port forwarding, public-address lookup or discovery service is installed. Legacy listeners and clients restrict unauthenticated/plaintext connections to trusted private/link/loopback addresses; a user-managed private reverse proxy or NAT setup still requires operator judgment. Do not advertise this prototype on WAN while security acceptance is failing.

The dedicated console consumes no player seat and uses the same start validator as the listen host. All configured hunters (1–6) must be ready; a human ghost is optional. `start`, `status`, `lock`, `unlock`, `invite`, `stop` are local STDIN only. Ghost AI remains active when no ghost is connected. Secure sessions open no UDP voice socket. This is a temporary case, not campaign hosting or a restart-persistent match. Normal castle-embedded LanLobbyPanel remains the player UI; no new launcher window was introduced. The TLS checkbox uses the protected `server-network` identity for listen-host mode. Secure room/password verification occurs before allocating a seat. Process-lifetime reconnect retains the pinned transport and binds stolen-token rejection to the original signing key; durable secure ticket/pin storage across UI restarts remains a gate.

For updates: stop through STDIN, verify exit/port release, back up data and the complete identity directory, build the reviewed source, replace the JAR, restart with unchanged identity and data paths, and run acceptance. For rollback retain the previous JAR and full coherent save/identity backup. Malefic keeps its existing validated world/checkpoint/backup migration constraints. No unauthenticated remote RCON or web administrator exists. OS systemd/Windows service wrappers and automatic restart policies are operator-managed and not installed by this change.
