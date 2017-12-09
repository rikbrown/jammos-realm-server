# jammos-auth-server
JVM-based Awesome MMO Server [Auth Server]

Implementation of the World of Warcraft Auth Server in Kotlin.

# Running

## Redis

Server requires Redis on port `15070`.

### MacOS

```
$ pwd
/Users/rik/Code/Jammos/jammos-realm-server/redis
$ ln -s /Users/rik/Code/Jammos/jammos-realm-server/redis/redis-realm-server.plist /Users/rik/Library/LaunchAgents/redis-realm-server.plist
$ launchctl load /Users/rik/Code/Jammos/jammos-realm-server/redis/redis-realm-server.plistlaunchctl load /Users/rik/Cod
$ redis-cli localhost:15070
```

# Developing

## IntellJ Configuration

* Install Spek plugin
* Enable formatter control (e.g. `// @formatter:off`)