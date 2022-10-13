import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.default
import kotlinx.cli.delimiter
import kotlinx.serialization.json.*
import kotlinx.serialization.serializer
import org.jetbrains.letsPlot.export.ggsave
import org.jetbrains.letsPlot.geom.geomPoint
import org.jetbrains.letsPlot.geom.geomSmooth
import org.jetbrains.letsPlot.intern.Plot
import org.jetbrains.letsPlot.letsPlot
import spin.*
import java.io.File
import java.util.*
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.Lock

//import javaspin.*

class Counter(var counter: Int = 0) {
    fun inc() {
        counter++
    }
}

fun testUniversal(numThreads: Int, counter: Universal<Counter>, limit: Int, executor: ExecutorService): Long {
    val start = System.currentTimeMillis()
    val barrier = CyclicBarrier(numThreads + 1)
    for (i in 0 until numThreads) {
        executor.submit {
            var res: Counter
            do {
                res = counter.apply {
                    if (this.counter < limit) {
                        inc()
                    }
                }
            } while (res.counter < limit)
            barrier.await()
        }
    }
    barrier.await()
    val end = System.currentTimeMillis()
    return end - start
}

fun delay() {
    val counter = Counter()
    val random = Random()
    repeat(random.nextInt(10)) {
        counter.inc()
    }
}

fun testRef(numThreads: Int, limit: Int = 1_000_000, executor: ExecutorService): Long {
    val barrier = CyclicBarrier(numThreads + 1)
    val start = System.currentTimeMillis()
    val counter = AtomicInteger(0)
    for (i in 0 until numThreads) {
        executor.submit {
            do {
                delay()
            } while (counter.getAndIncrement() < limit)
            barrier.await()
        }
    }
    barrier.await()
    val end = System.currentTimeMillis()
    return end - start
}

fun testLock(numThreads: Int, lock: Lock, limit: Int = 1_000_000, executor: ExecutorService): Long {
    val counter = Counter(0)
    val barrier = CyclicBarrier(numThreads + 1)
    val start = System.currentTimeMillis()
    for (i in 0 until numThreads) {
        executor.submit {
            do {
                lock.lock()
                try {
                    if (counter.counter < limit) {
                        counter.inc()
                    }
                } finally {
                    lock.unlock()
                }
                delay()
            } while (counter.counter < limit)
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
    plot += geomPoint(size = 0.5) { color = "name" }
    plot += geomSmooth(method = "loess", span = 0.5, size = 0.5, ymin = 0, alpha = 0) { color = "name"; group = "name" }
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
    val universalList = listOf<(Int) -> Universal<Counter>>(
        { LFUniversal<Counter>(it) { Counter() } },
        { WFUniversal<Counter>(it) { Counter() } },
        { LFUniversalBack<Counter>(it, { Counter() }, { obj -> Counter(obj.counter) } ) },
        { WFUniversalBack<Counter>(it, { Counter() }, { obj -> Counter(obj.counter) } ) },
    )
    val universalNames = listOf(
        "LFUniversal",
        "WFUniversal",
        "LFUniversalBack",
        "WFUniversalBack",
    )
    val universalNameFactoryMap = universalNames.zip(universalList).toMap()
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
    ).delimiter(",")//.default(lockNames)
    val universals = parser.option(
        ArgType.Choice<String>(universalNames, { it }, { it }),
        fullName = "universals",
        shortName = "U",
        description = "Universals to test"
    ).delimiter(",")//.default(universalNames)
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
        repeat(numTrials.value) {
            val result = testRef(numThreads, limit.value, executor)
            data.getOrPut("time") { mutableListOf() }.add(result)
            data.getOrPut("numThreads") { mutableListOf() }.add(numThreads)
            data.getOrPut("name") { mutableListOf() }.add("Ref")
            println("Test for $numThreads threads reference takes $result milliseconds!")
        }
        for (name in universals.value) {
            repeat(numTrials.value) {
                ThreadID.reset()
                val counter = universalNameFactoryMap[name]!!(numThreads)
                val result = testUniversal(numThreads, counter, limit.value, executor)
                data.getOrPut("time") { mutableListOf() }.add(result)
                data.getOrPut("numThreads") { mutableListOf() }.add(numThreads)
                data.getOrPut("name") { mutableListOf() }.add(name)
                println("Test for $numThreads threads with $name takes $result milliseconds!")
            }
        }
        for (name in locks.value) {
            val test: (Int) -> Unit = { backoff: Int ->
                repeat(numTrials.value) {
                    ThreadID.reset()
                    val lock = lockNameFactoryMap[name]!!(numThreads)
                    val result = testLock(numThreads, lock, limit.value, executor)
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
    val dataJson = data.toJsonString()
    File("${output.value}.json").writeText(dataJson)
    val fig = createPlot(data)
    ggsave(fig, "${output.value}.${outputFormat.value}")
    println("Finish plotting!")
}