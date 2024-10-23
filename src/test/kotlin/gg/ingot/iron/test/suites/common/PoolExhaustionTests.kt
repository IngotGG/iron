package gg.ingot.iron.test.suites.common

import gg.ingot.iron.test.IronTest
import io.kotest.core.annotation.AutoScan
import io.kotest.core.spec.style.DescribeSpec
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch

@AutoScan
class PoolExhaustionTests: DescribeSpec({
    describe("Connection Pool Exhaustion") {
        it("shouldn't exhaust the connection pool") {
            val iron = IronTest.sqlite(IronTest.pooled())

            val jobs = List(10) {
                launch {
                    iron.use {
                        it.createStatement().execute("SELECT 1;")
                    }
                }
            }

            jobs.joinAll()
            iron.close()
        }
    }
})