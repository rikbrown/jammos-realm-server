package net.jammos.realmserver

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
import net.jammos.realmserver.auth.InMemoryAuthDao
import net.jammos.realmserver.auth.Username.Username.username
import net.jammos.realmserver.auth.crypto.CryptoConstants
import net.jammos.realmserver.auth.crypto.CryptoManager
import net.jammos.realmserver.network.AuthServerHandler
import net.jammos.realmserver.network.SessionHandler
import net.jammos.realmserver.network.message.coding.ClientAuthMessageDecoder
import net.jammos.realmserver.network.message.coding.ServerAuthMessageEncoder
import net.jammos.realmserver.realm.InMemoryRealmDao
import net.jammos.realmserver.session.InMemorySessionManager
import java.net.InetAddress
import java.time.Instant.now

class AuthServer {
    companion object {
        private val TIMEOUT = 10
        private val PORT = 3724

        private val cryptoManager = CryptoManager(constants = CryptoConstants())
        private val authDao = InMemoryAuthDao(cryptoManager = cryptoManager)
        private val realmDao = InMemoryRealmDao()
        private val sessionManager = InMemorySessionManager()
        private val authManager = AuthManager(cryptoManager, authDao)

        init {
            authDao.createUser(username("rikbrown"), "test1234")
            authDao.suspendUser(
                    user = authDao.createUser(username("banned"), "foo"),
                    end = null)
            authDao.suspendUser(
                    user = authDao.createUser(username("banned"), "foo"),
                    end = now())
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
                                        SessionHandler(sessionManager),
                                        AuthServerHandler(sessionManager, authManager, realmDao))
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

