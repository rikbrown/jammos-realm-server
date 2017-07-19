package net.jammos.realmserver.utils.types

open class ComparableByteArray(bytes: ByteArray) {
    override fun equals(other: Any?) = other is ComparableByteArray && bytes contentEquals other.bytes
    override fun hashCode() = bytes.contentHashCode()
}