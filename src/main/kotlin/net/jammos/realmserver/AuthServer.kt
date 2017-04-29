package net.jammos.realmserver

import net.jammos.realmserver.auth.InMemoryAuthDao
import net.jammos.realmserver.auth.Username.Username.username
import net.jammos.realmserver.auth.crypto.CryptoConstants
import net.jammos.realmserver.auth.crypto.CryptoManager
import net.jammos.realmserver.realms.InMemoryRealmDao
import java.net.ServerSocket
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class AuthServer {
    companion object {
        val executorService: ExecutorService = Executors.newCachedThreadPool()
        val serverSocket = ServerSocket(3724)
        val cryptoManager = CryptoManager(constants = CryptoConstants())
        val authDao = InMemoryAuthDao(cryptoManager)
        val realmDao = InMemoryRealmDao()

        init {
            authDao.createUser(username("rikbrown"), "test1234")
            authDao.banUser(authDao.createUser(username("banned"), "foo"))
        }

        @JvmStatic fun main(args: Array<String>) {
            while (true) {
                val socket = serverSocket.accept();
                executorService.submit {
                    try {
                        AuthClientSocket(socket,
                                authDao = authDao,
                                realmDao = realmDao,
                                cryptoManager = cryptoManager).run()
                    } catch (e: Exception) {
                        e.printStackTrace()
                        socket.close()
                    }
                }
            }
        }
    }



}

