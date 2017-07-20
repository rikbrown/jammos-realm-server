package net.jammos.realmserver.utils.types

open class ComparableByteArray(val bytes: ByteArray) {
    override fun equals(other: Any?) = other is ComparableByteArray && bytes contentEquals other.bytes
    override fun hashCode() = bytes.contentHashCode()
}