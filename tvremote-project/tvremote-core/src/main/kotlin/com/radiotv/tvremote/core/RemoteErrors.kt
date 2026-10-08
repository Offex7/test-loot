package com.radiotv.tvremote.core

open class RemoteException(message: String, cause: Throwable? = null) : Exception(message, cause)
class PairingException(message: String, cause: Throwable? = null) : RemoteException(message, cause)
