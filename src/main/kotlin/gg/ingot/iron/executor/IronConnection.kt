package gg.ingot.iron.executor

import gg.ingot.iron.executor.impl.BlockingIronExecutor
import gg.ingot.iron.executor.impl.CompletableIronExecutor
import gg.ingot.iron.executor.impl.CoroutineIronExecutor

/**
 * Represents the entrypoint for executing queries on the database.
 * @author santio
 * @see CoroutineIronExecutor
 * @see BlockingIronExecutor
 * @see CompletableIronExecutor
 */
interface IronConnection