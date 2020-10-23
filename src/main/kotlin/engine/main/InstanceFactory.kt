package engine.main

import kotlin.reflect.KClass
import kotlin.reflect.full.createInstance


interface InstanceFactory {
    fun <T : Any> getInstance(clazz: KClass<T>): T
}

class SimpleInstanceFactory : InstanceFactory {
    override fun <T : Any> getInstance(clazz: KClass<T>): T {
        return clazz.createInstance()
    }
}
