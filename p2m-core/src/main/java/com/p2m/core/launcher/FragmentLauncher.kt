package com.p2m.core.launcher

import com.p2m.annotation.module.api.ApiLauncher
import com.p2m.core.internal.launcher.InternalFragmentLauncher
import kotlin.reflect.KProperty

/**
 * A launcher of `Fragment`.
 *
 * For example, has a `Fragment` of showing home in `Main` module:
 * ```kotlin
 * @ApiLauncher("Home")
 * class HomeFragment : Fragment()
 * ```
 *
 * then get a instance for launch in external module:
 * ```kotlin
 * val homeFragment = P2M.apiOf(Main)
 *      .launcher
 *      .fragmentOfHome
 *      .navigation()
 * ```
 *
 * @see ApiLauncher
 */
interface FragmentLauncher<T> : Launcher {
    companion object {
        fun <T> delegate(createBlock: () -> T): Lazy<FragmentLauncher<T>> =
            lazy(LazyThreadSafetyMode.NONE) { InternalFragmentLauncher(createBlock) }
    }

    /**
     * Create a fragment for that class annotated by [ApiLauncher].
     *
     * @return a instance.
     */
    fun navigation(): T
}

/**
 * A block for launch fragment.
 */
typealias LaunchFragmentBlock<T> = (fragment: T) -> Unit