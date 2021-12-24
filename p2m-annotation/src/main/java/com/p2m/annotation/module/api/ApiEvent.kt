package com.p2m.annotation.module.api

/**
 * A class uses this annotation, it will generate a event interface for event of `Api` area,
 * that class member property only use [ApiEventField] annotation to take effect.
 *
 * Use `P2M.apiOf(${moduleName}::class.java).event` to get `event` instance,
 * that `moduleName` is defined in `settings.gradle`.
 *
 * The annotation can only be used once within same module scope.
 *
 * For example, define the event holder for successful login in `Account` module:
 * ```kotlin
 * @ApiEvent
 * interface Event {
 *      @ApiEventField(eventOn = EventOn.BACKGROUND, externalMutable = false)
 *      val loginSuccess : Unit
 * }
 * ```
 *
 * then observe in external module:
 * ```kotlin
 * P2M.apiOf(Account)
 *      .event
 *      .loginSuccess
 *      .observe(Observe { _ ->
 *          jump after login success...
 *      })
 * ```
 *
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
annotation class ApiEvent

/**
 *  A field uses this annotation, it will generate different event holder property
 * according to [eventOn] and [externalMutable].
 *
 * Default is [EventOn.MAIN] + immutable from external.
 *
 * Use only in class annotated by [ApiEvent].
 *
 * @property eventOn specified thread to receive event.
 * @property externalMutable mutable from external, if true that External module
 * support call setValue and postValue.
 */
@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.BINARY)
annotation class ApiEventField(val eventOn: EventOn = EventOn.MAIN, val externalMutable: Boolean = false)

/**
 * Which thread to maintain event do you want.
 */
enum class EventOn{
    MAIN,               // emit event on main thread, occupy main thread resources.
    BACKGROUND,         // emit event on background thread, not occupy main thread resources.
}