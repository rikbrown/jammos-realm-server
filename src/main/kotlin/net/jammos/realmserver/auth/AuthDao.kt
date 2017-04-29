package net.jammos.realmserver.auth

import net.jammos.realmserver.auth.crypto.CryptoManager
import net.jammos.realmserver.utils.ByteArrays.randomBytes

interface AuthDao {
    fun createUser(username: Username, password: String): User
    fun getUser(username: Username): User?
    fun banUser(user: User): User?
}

class InMemoryAuthDao(private val cryptoManager: CryptoManager): AuthDao {
    val users = HashMap<Username, User>()

    override fun createUser(username: Username, password: String): User {

        val salt = randomBytes(32)
        val passwordUpper = password.toUpperCase()

        val loginHash = cryptoManager.createPrivateKey(
                username.toByteArray(Charsets.UTF_8),
                passwordUpper.toByteArray(Charsets.UTF_8),
                salt)

        val user = User(
                username = username,
                salt = salt,
                verifier = cryptoManager.createUserVerifier(loginHash))
        users[username] = user

        return user
    }

    override fun getUser(username: Username): User? {
        return users[username]
    }

    override fun banUser(user: User): User? {
        return users[user.username]?.copy(isBanned = true)?.let {
            users[user.username] = it
            return it
        }
    }

}