package tech.stit.mobile.core.lib

import platform.Foundation.NSUserDefaults

class UserDefaultsSettings(private val defaults: NSUserDefaults = NSUserDefaults.standardUserDefaults) : Settings {
    override suspend fun getString(key: String): String? = defaults.stringForKey(key)

    override suspend fun putString(key: String, value: String) = defaults.setObject(value, forKey = key)

    override suspend fun remove(key: String) = defaults.removeObjectForKey(key)
}
