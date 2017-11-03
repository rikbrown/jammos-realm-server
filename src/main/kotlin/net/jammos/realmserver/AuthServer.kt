package net.jammos.realmserver

import com.lambdaworks.redis.RedisClient
import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.ChannelInitializer
import io.netty.channel.ChannelOption
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.handler.logging.LogLevel
import io.netty.handler.logging.LoggingHandler
import io.netty.handler.timeout.ReadTimeoutHandler
import net.jammos.realmserver.auth.AuthManager
import net.jammos.realmserver.network.handler.LogonChallengeHandler
import net.jammos.realmserver.network.handler.LogonProofHandler
import net.jammos.realmserver.network.handler.RealmListHandler
import net.jammos.realmserver.network.message.coding.ClientAuthMessageDecoder
import net.jammos.realmserver.network.message.coding.ServerAuthMessageEncoder
import net.jammos.realmserver.realm.*
import net.jammos.utils.auth.crypto.CryptoManager
import net.jammos.utils.auth.dao.RedisAuthDao
import net.jammos.utils.types.InternetAddress
import java.net.InetAddress

class AuthServer {
    companion object {
        private val TIMEOUT = 10
        private val PORT = 3724

        private val redis = RedisClient.create("redis://localhost")
        private val cryptoManager = CryptoManager()
        private val authDao = RedisAuthDao(redis, cryptoManager)
        private val realmDao = RedisRealmDao(redis)
        private val authManager = AuthManager(cryptoManager, authDao)

        init {
            realmDao.updateRealm(Realm(
                    id = RealmId("test1"),
                    name = "Rank 11 Druids Only",
                    address = InternetAddress("127.0.0.1", 1234),
                    realmType = RealmType.PVP))
            realmDao.updateRealm(Realm(
                    id = RealmId("test2"),
                    name = "Synergy (Golden Gods)",
                    address = InternetAddress("127.0.0.1", 1234),
                    realmType = RealmType.RPPVP))
            realmDao.updateRealm(Realm(
                    id = RealmId("test3"),
                    name = "Joe plz come here",
                    address = InternetAddress("127.0.0.1", 1234),
                    realmFlags = setOf(RealmFlag.REALM_FLAG_NEW_PLAYERS)))
            realmDao.updateRealm(Realm(
                    id = RealmId("test4"),
                    name = "RIP Feenix",
                    address = InternetAddress("127.0.0.1", 1234),
                    realmFlags = setOf(RealmFlag.REALM_FLAG_OFFLINE)))
        }

        @JvmStatic fun main(args: Array<String>) {

            // Configure the server.
            val bossGroup = NioEventLoopGroup(1)
            val workerGroup = NioEventLoopGroup()
            try {
                val b = ServerBootstrap()

                b.group(bossGroup, workerGroup)
                        .channel(NioServerSocketChannel::class.java)
                        .option(ChannelOption.SO_BACKLOG, 100)
                        .option(ChannelOption.TCP_NODELAY, true)
                        .option(ChannelOption.SO_KEEPALIVE, true)
                        .option(ChannelOption.SO_REUSEADDR, true)
                        .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 100)
                        .handler(LoggingHandler(LogLevel.INFO))
                        .childHandler(object : ChannelInitializer<SocketChannel>() {
                            @Throws(Exception::class)
                            public override fun initChannel(ch: SocketChannel) {
                                val p = ch.pipeline()
                                p.addLast(
                                        ClientAuthMessageDecoder(),
                                        ServerAuthMessageEncoder(),
                                        ReadTimeoutHandler(TIMEOUT),

                                        LogonChallengeHandler(authManager),
                                        LogonProofHandler(authManager),
                                        RealmListHandler(authManager, realmDao))
                            }
                        })

                // Start the server.
                val hostAddress = InetAddress.getLoopbackAddress()
                val f = b.bind(hostAddress, PORT).sync()



                // Wait until the server socket is closed.
                f.channel().closeFuture().sync()
            } finally {
                // Shut down all event loops to terminate all threads.
                bossGroup.shutdownGracefully()
                workerGroup.shutdownGracefully()
            }

        }

    }
}

