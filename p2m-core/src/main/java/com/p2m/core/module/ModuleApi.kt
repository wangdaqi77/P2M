package com.p2m.core.module

import com.p2m.annotation.module.api.*

/**
 * A [Module] has one [ModuleApi] only.
 *
 * It is like a window of a [Module].
 *
 * @property launcher launch Activity、Fragment、Service, link to [ApiLauncher].
 * @property service call some feature, link to [ApiService].
 * @property event observe some event, related to [ApiEvent].
 */
interface ModuleApi<LAUNCHER : ModuleLauncher, SERVICE : ModuleService, EVENT : ModuleEvent> {
     val launcher: LAUNCHER
     val service: SERVICE
     val event: EVENT

    operator fun component1() = launcher
    operator fun component2() = service
    operator fun component3() = event
}

class EmptyModuleApi(
    override val launcher: EmptyModuleLauncher = EmptyModuleLauncher,
    override val service: EmptyModuleService = EmptyModuleService,
    override val event: EmptyModuleEvent = EmptyModuleEvent
) : ModuleApi<EmptyModuleLauncher, EmptyModuleService, EmptyModuleEvent>

interface ModuleLauncher

interface ModuleService

interface ModuleEvent

object EmptyModuleLauncher : ModuleLauncher

object EmptyModuleService : ModuleService

object EmptyModuleEvent : ModuleEvent