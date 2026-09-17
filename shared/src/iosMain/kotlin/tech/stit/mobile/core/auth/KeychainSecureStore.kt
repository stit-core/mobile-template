package tech.stit.mobile.core.auth

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import platform.CoreFoundation.CFDictionaryAddValue
import platform.CoreFoundation.CFDictionaryCreateMutable
import platform.CoreFoundation.CFMutableDictionaryRef
import platform.CoreFoundation.CFRelease
import platform.CoreFoundation.CFTypeRef
import platform.CoreFoundation.CFTypeRefVar
import platform.CoreFoundation.kCFBooleanFalse
import platform.CoreFoundation.kCFBooleanTrue
import platform.CoreFoundation.kCFTypeDictionaryKeyCallBacks
import platform.CoreFoundation.kCFTypeDictionaryValueCallBacks
import platform.Foundation.CFBridgingRelease
import platform.Foundation.CFBridgingRetain
import platform.Foundation.NSData
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.Foundation.dataUsingEncoding
import platform.Security.SecItemAdd
import platform.Security.SecItemCopyMatching
import platform.Security.SecItemDelete
import platform.Security.errSecItemNotFound
import platform.Security.errSecSuccess
import platform.Security.kSecAttrAccessible
import platform.Security.kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly
import platform.Security.kSecAttrAccount
import platform.Security.kSecAttrService
import platform.Security.kSecAttrSynchronizable
import platform.Security.kSecClass
import platform.Security.kSecClassGenericPassword
import platform.Security.kSecMatchLimit
import platform.Security.kSecMatchLimitOne
import platform.Security.kSecReturnData
import platform.Security.kSecValueData

// AfterFirstUnlockThisDeviceOnly и без iCloud-синхронизации: refresh-токен привязан к устройству (07 §1.3b).
@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
class KeychainSecureStore(private val service: String = "tech.stit.mobile") : SecureStore {
    override suspend fun read(key: String): String? = memScoped {
        query(key) { query ->
            CFDictionaryAddValue(query, kSecReturnData, kCFBooleanTrue)
            CFDictionaryAddValue(query, kSecMatchLimit, kSecMatchLimitOne)
            val result = alloc<CFTypeRefVar>()
            if (SecItemCopyMatching(query, result.ptr) != errSecSuccess) return@query null
            val data = CFBridgingRelease(result.value) as? NSData ?: return@query null
            NSString.create(data, NSUTF8StringEncoding)?.toString()
        }
    }

    override suspend fun write(key: String, value: String) {
        remove(key)
        val data =
            NSString.create(string = value).dataUsingEncoding(NSUTF8StringEncoding) ?: error("Keychain: строка не кодируется в UTF-8")
        query(key) { query ->
            withBridged(data) { CFDictionaryAddValue(query, kSecValueData, it) }
            CFDictionaryAddValue(query, kSecAttrAccessible, kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly)
            val status = SecItemAdd(query, null)
            check(status == errSecSuccess) { "Keychain: SecItemAdd вернул $status" }
        }
    }

    override suspend fun remove(key: String) {
        query(key) { query ->
            val status = SecItemDelete(query)
            check(status == errSecSuccess || status == errSecItemNotFound) { "Keychain: SecItemDelete вернул $status" }
        }
    }

    private inline fun <T> query(key: String, block: (CFMutableDictionaryRef) -> T): T {
        val query = checkNotNull(CFDictionaryCreateMutable(null, 0, kCFTypeDictionaryKeyCallBacks.ptr, kCFTypeDictionaryValueCallBacks.ptr))
        return try {
            CFDictionaryAddValue(query, kSecClass, kSecClassGenericPassword)
            withBridged(service) { CFDictionaryAddValue(query, kSecAttrService, it) }
            withBridged(key) { CFDictionaryAddValue(query, kSecAttrAccount, it) }
            CFDictionaryAddValue(query, kSecAttrSynchronizable, kCFBooleanFalse)
            block(query)
        } finally {
            CFRelease(query)
        }
    }

    // Словарь удерживает значение сам (kCFTypeDictionaryValueCallBacks); лишний retain от bridging снимается сразу.
    private inline fun withBridged(value: Any, block: (CFTypeRef?) -> Unit) {
        val ref = CFBridgingRetain(value)
        try {
            block(ref)
        } finally {
            CFBridgingRelease(ref)
        }
    }
}
