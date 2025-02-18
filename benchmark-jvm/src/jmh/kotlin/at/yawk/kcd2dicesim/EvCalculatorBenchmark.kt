package at.yawk.kcd2dicesim

import org.openjdk.jmh.annotations.Benchmark
import org.openjdk.jmh.annotations.BenchmarkMode
import org.openjdk.jmh.annotations.Fork
import org.openjdk.jmh.annotations.Measurement
import org.openjdk.jmh.annotations.Mode
import org.openjdk.jmh.annotations.OutputTimeUnit
import org.openjdk.jmh.annotations.Warmup
import java.util.concurrent.TimeUnit

@Fork(1)
@Warmup(iterations = 5, time = 10)
@Measurement(iterations = 5, time = 10)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
open class EvCalculatorBenchmark {
    @Benchmark
    fun test(): EvCalculator.Ev {
        val bag = DieBag.of(
            listOf(
                SpecialDie.NORMAL_DIE,
                SpecialDie.NORMAL_DIE,
                SpecialDie.NORMAL_DIE,
                SpecialDie.NORMAL_DIE,
                SpecialDie.NORMAL_DIE,
                SpecialDie.NORMAL_DIE,
            )
        )
        return EvCalculator(Score(3000), bag)
            .calculateEv(Score(0), bag)
    }
}