package net.jammos.realmserver

import com.google.common.io.LittleEndianDataInputStream
import com.google.common.io.LittleEndianDataOutputStream
import net.jammos.realmserver.auth.AuthDao
import net.jammos.realmserver.auth.crypto.CryptoManager
import net.jammos.realmserver.handler.Handler
import net.jammos.realmserver.handler.LogonChallengeHandler
import net.jammos.realmserver.realms.RealmDao
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.net.Socket

class AuthClientSocket(
        val socket: Socket,
        cryptoManager: CryptoManager,
        authDao: AuthDao,
        realmDao: RealmDao) {
    private val inputStream = LittleEndianDataInputStream(BufferedInputStream(socket.getInputStream()))
    private val outputStream = LittleEndianDataOutputStream(BufferedOutputStream(socket.getOutputStream()))
    private var handler: Handler? = LogonChallengeHandler(inputStream, outputStream,
            cryptoManager = cryptoManager,
            authDao = authDao,
            realmDao = realmDao)

    fun run() {
        System.err.println("Accepting connection");

        while (handler != null) {
            System.err.println("Handling connection using " + handler)
            handler = handler!!.handle()
            outputStream.flush()
            System.err.println("Handled and flushed")
        }

        System.err.println("No more handlers, closing socket")
        done()

        /*val command = inputStream.read()
        System.out.println("Command: " + command)

        val error = inputStream.read()
        val packetSize = inputStream.readShort()

        val challenge = AuthLogonChallenge.read(inputStream)
        System.out.println(challenge)

        AuthLogonChallengeResponse(AuthResult.BANNED).write(outputStream)*/
    }

    fun done() {
        Thread.sleep(500)
        socket.close()
    }

}