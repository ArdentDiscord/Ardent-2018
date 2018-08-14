package com.ardentbot.core.commands

import com.ardentbot.core.ArdentRegister
import com.ardentbot.core.translation.Language
import com.ardentbot.core.utils.UnknownModuleFoundException
import org.reflections.Reflections

class CommandHolder(val register: ArdentRegister) {
    val modules = hashMapOf<Module, MutableList<Command>>()
    val commands get() = modules.values.flatten()

    private val reflections = Reflections("com.ardentbot")

    init {
        reflections.getSubTypesOf(Module::class.java).forEach { moduleClass ->
            modules[moduleClass.constructors[0].newInstance() as Module] = mutableListOf()
        }

        reflections.getTypesAnnotatedWith(ModuleMapping::class.java).filter { !it.isAnnotationPresent(Excluded::class.java) }.forEach { clazz ->
            val moduleName = clazz.getDeclaredAnnotation(ModuleMapping::class.java).name
            if (!modules.map { it.key.id }.contains(moduleName)) throw UnknownModuleFoundException("$moduleName was not " +
                    "found as a module. Expected one of: ${modules.map { it.key.id }}")
            val command = clazz.constructors[0].newInstance() as Command
            command.description = command.translate("${command.name}.description", Language.ENGLISH, register)
            clazz.declaredFields.filter { it.isAccessible = true; it.name.startsWith("example") }
                    .map { it.get(command) as String }.let { command.ex.addAll(it) }
            clazz.declaredFields.filter {
                it.isAccessible = true; it.get(command) is Precondition
            }.forEach { precondition -> command.preconditions.add(precondition.get(command) as Precondition) }

            clazz.declaredFields.filter { it.isAccessible = true; it.get(command) is Argument || it.get(command) is FlagModel }
                    .forEach { information ->
                        val field = information.get(command)
                        when (field) {
                            is Argument -> command.arguments.add(field)
                            is FlagModel -> command.flags.add(field)
                        }
                    }
            modules[modules.keys.first { it.id == moduleName }]!!.add(command)
        }
    }

    fun getCommandsFor(moduleId: String) = modules.keys.firstOrNull { it.id == moduleId }
            ?.let { modules[it] }
            ?: throw UnknownModuleFoundException("Unknown module specified: $moduleId")

    fun getModuleFor(command: Command) = modules.entries.first { it.value.contains(command) }.key

    fun getCommand(name: String): Command? = modules.values.flatten().firstOrNull { name == it.name }
    fun getModule(name: String): Module? = modules.keys.firstOrNull { it.name == name || it.id == name }
}