package cn.com.omnimind.omnibot.devserver.contract

import kotlin.reflect.KClass

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class CommandInfo(
    val name: String,
    val description: String,
    val argNames: Array<String>,
    val responseType: KClass<*>,
)

data class Command(
    val name: String,
    val description: String,
    val argNames: List<String>,
)
