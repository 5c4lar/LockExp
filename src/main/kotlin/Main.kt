import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.default
import kotlinx.cli.delimiter
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import kotlinx.serialization.serializer
import org.jetbrains.letsPlot.export.ggsave
import org.jetbrains.letsPlot.geom.geomPoint
import org.jetbrains.letsPlot.geom.geomSmooth
import org.jetbrains.letsPlot.intern.Plot
import org.jetbrains.letsPlot.letsPlot
import spin.*
import java.io.File
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.locks.Lock

//import javaspin.*

class Counter(private val lock: Lock, private val limit: Int = 1_000_000) {
    var counter = 0
        private set

    fun reachLimit() {
        while (counter < limit) {
            lock.lock()
            try {
                if (counter < limit) {
                    counter++
                }
            } finally {
                lock.unlock()
            }
        }
    }
}

class LUCounter

fun testForThreads(numThreads: Int, lock: Lock, limit: Int = 1_000_000, executor: ExecutorService): Long {
    val counter = Counter(lock, limit)
    val start = System.currentTimeMillis()
    val barrier = CyclicBarrier(numThreads + 1)
    for (i in 0 until numThreads) {
        executor.submit {
            counter.reachLimit()
            if (lock is HCLHLock) {
                lock.stop()
            }
            barrier.await()
        }
    }
    barrier.await()
    val end = System.currentTimeMillis()
    val totalTime = end - start
    assert(counter.counter == limit)
    return totalTime
}

fun Any?.toJsonElement(): JsonElement = when (this) {
    null -> JsonNull
    is JsonElement -> this
    is Number -> JsonPrimitive(this)
    is Boolean -> JsonPrimitive(this)
    is String -> JsonPrimitive(this)
    is Array<*> -> JsonArray(map { it.toJsonElement() })
    is List<*> -> JsonArray(map { it.toJsonElement() })
    is Map<*, *> -> JsonObject(map { it.key.toString() to it.value.toJsonElement() }.toMap())
    else -> Json.encodeToJsonElement(serializer(this::class.javaObjectType), this)
}

fun Any?.toJsonString(): String = this.toJsonElement().toString()

fun createPlot(data: Map<String, Any>): Plot {
    var plot = letsPlot(data) { x = "numThreads"; y = "time"; color = "name" }
    plot += geomPoint { color = "name" }
    plot += geomSmooth(method = "loess", span = 0.5, size = 1.0, ymin = 0, alpha = 0) { color = "name"; group = "name" }
    return plot
}

fun main(args: Array<String>) {

    val lockList = listOf<(Int) -> Lock>(
        { ALock(it) },
        { BackoffLock() },
        { CLHLock() },
        { CompositeLock() },
        { CompositeFastPathLock() },
        { HBOLock() },
        { HCLHLock() },
        { MCSLock() },
        { TASLock() },
        { TTASLock() },
        { TOLock() },
    )
    val lockNames = listOf(
        "ALock",
        "BackoffLock",
        "CLHLock",
        "CompositeLock",
        "CompositeFastPathLock",
        "HBOLock",
        "HCLHLock",
        "MCSLock",
        "TASLock",
        "TTASLock",
        "TOLock"
    )
    val lockNameFactoryMap = lockNames.zip(lockList).toMap()
    val parser = ArgParser("Locks Experiments")
    val minThreads =
        parser.option(ArgType.Int, fullName = "min", shortName = "m", description = "Min number of threads").default(1)
    val maxThreads =
        parser.option(ArgType.Int, fullName = "max", shortName = "M", description = "Max number of threads").default(8)
    val step =
        parser.option(ArgType.Int, fullName = "step", shortName = "s", description = "Step for threads").default(1)
    val limit = parser.option(ArgType.Int, fullName = "limit", shortName = "l", description = "Limit for counter")
        .default(1_000_000)
    val numTrials =
        parser.option(ArgType.Int, fullName = "repeats", shortName = "r", description = "Number of trials").default(10)
    val locks = parser.option(
        ArgType.Choice<String>(lockNames, { it }, { it }),
        fullName = "locks",
        shortName = "L",
        description = "Locks to test"
    ).delimiter(",").default(lockNames)
    val output =
        parser.option(ArgType.String, fullName = "output", shortName = "o", description = "Output file").default("plot")
    val outputFormat = parser.option(
        ArgType.Choice<String>(listOf("png", "html", "svg", "jpeg", "tiff"), { it }, { it }),
        fullName = "format",
        shortName = "f",
        description = "Output format"
    ).default("html")
    val backoffMin = parser.option(ArgType.Int, fullName = "minBackoff", shortName = "b", description = "Min backoff")
        .default(1)
    val backoffTrials =
        parser.option(ArgType.Int, fullName = "backoffTrials", shortName = "T", description = "backoff trials")
            .default(10)
    parser.parse(args)
    val backoffLocks = listOf(
        "BackoffLock",
        "CompositeLock",
        "CompositeFastPathLock"
    )
    val data = mutableMapOf<String, MutableList<Any>>()
    for (numThreads in minThreads.value..maxThreads.value step step.value) {
        val executor = Executors.newFixedThreadPool(numThreads)
        for (name in locks.value) {
            val test: (Int) -> Unit = { backoff: Int ->
                repeat(numTrials.value) {
                    ThreadID.reset()
                    val lock = lockNameFactoryMap[name]!!.invoke(numThreads)
                    val result = testForThreads(numThreads, lock, limit.value, executor)
                    data.getOrPut("time") { mutableListOf() }.add(result)
                    data.getOrPut("numThreads") { mutableListOf() }.add(numThreads)
                    val id = if (name in backoffLocks
                    ) "${name}_${backoff}" else name
                    data.getOrPut("name") { mutableListOf() }.add(id)
                    println("Test for $numThreads threads with $id takes $result milliseconds!")
                }
            }
            if (name in backoffLocks) {
                var backoff = backoffMin.value
                repeat(backoffTrials.value) {
                    Backoff.MIN_DELAY = backoff
                    test(backoff)
                    backoff *= 2
                }
                Backoff.MIN_DELAY = null
            } else {
                test(1)
            }
        }
        executor.shutdownNow()
    }
    println("Finish processing!")
    // dump data as json string to file
    val dataJson = data.toJsonString()
    File("${output.value}.json").writeText(dataJson)
    val fig = createPlot(data)
    ggsave(fig, "${output.value}.${outputFormat.value}")
    println("Finish plotting!")
}