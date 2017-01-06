package redux.logger

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.atLeastOnce
import com.nhaarman.mockito_kotlin.times
import com.nhaarman.mockito_kotlin.verify
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import org.junit.platform.runner.JUnitPlatform
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.mock
import redux.api.Store
import redux.logger.Logger.Entry
import redux.logger.helper.State
import redux.logger.helper.uninitialized
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import org.mockito.Mockito.`when` as upon

/*
 * Copyright (C) 2016 Michael Pardo
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

@RunWith(JUnitPlatform::class)
class CreateLoggerMiddlewareTest : Spek({

    @Suppress("UNCHECKED_CAST")
    describe("createLoggerMiddleware") {

        it("excludes diff by default") {
            val store = mock(Store::class.java) as Store<State>
            val logger = mock(Logger::class.java) as Logger<State>
            val captor = ArgumentCaptor.forClass(Entry::class.java) as ArgumentCaptor<Entry<State>>
            val loggerMiddleware = createLoggerMiddleware(logger)

            upon(store.state).thenReturn(State(0))
            loggerMiddleware.dispatch(store, {}, Unit)

            verify(logger, atLeastOnce()).log(captor.capture() ?: uninitialized())
            assertNull(captor.value.diff)

            upon(store.state).thenReturn(State(1))
            loggerMiddleware.dispatch(store, {}, Unit)

            verify(logger, atLeastOnce()).log(captor.capture() ?: uninitialized())
            assertNull(captor.value.diff)
        }

        it("obeys diff option") {
            val store = mock(Store::class.java) as Store<State>
            val logger = mock(Logger::class.java) as Logger<State>
            val captor = ArgumentCaptor.forClass(Entry::class.java) as ArgumentCaptor<Entry<State>>
            val loggerMiddleware = createLoggerMiddleware(logger, diff = true)

            upon(store.state).thenReturn(State(0))
            loggerMiddleware.dispatch(store, {}, Unit)

            verify(logger, atLeastOnce()).log(captor.capture() ?: uninitialized())
            assertNotNull(captor.value.diff)

            upon(store.state).thenReturn(State(1))
            loggerMiddleware.dispatch(store, {}, Unit)

            verify(logger, atLeastOnce()).log(captor.capture() ?: uninitialized())
            assertNotNull(captor.value.diff)
        }

        it("obeys predicate") {
            val store = mock(Store::class.java) as Store<State>
            val logger = mock(Logger::class.java) as Logger<State>

            var predicateCalls = 0
            val predicate = { action: Any, store: Store<State> ->
                predicateCalls++
                store.state.count < 5
            }
            val loggerMiddleware = createLoggerMiddleware(logger, predicate = predicate)

            upon(store.state).thenReturn(State(0))
            loggerMiddleware.dispatch(store, {}, Unit)

            upon(store.state).thenReturn(State(10))
            loggerMiddleware.dispatch(store, {}, Unit)

            verify(logger).log(any())
            assertEquals(2, predicateCalls)
        }

        it("obeys diff predicate") {
            val store = mock(Store::class.java) as Store<State>
            val logger = mock(Logger::class.java) as Logger<State>
            val captor = ArgumentCaptor.forClass(Entry::class.java) as ArgumentCaptor<Entry<State>>

            var diffPredicateCalls = 0
            val diffPredicate = { action: Any, store: Store<State> ->
                diffPredicateCalls++
                store.state.count < 5
            }
            val loggerMiddleware = createLoggerMiddleware(
                logger,
                diff = true,
                diffPredicate = diffPredicate
            )

            upon(store.state).thenReturn(State(0))
            loggerMiddleware.dispatch(store, {}, Unit)

            verify(logger, times(1)).log(captor.capture() ?: uninitialized())
            assertNotNull(captor.value.diff)

            upon(store.state).thenReturn(State(10))
            loggerMiddleware.dispatch(store, {}, Unit)

            verify(logger, times(2)).log(captor.capture() ?: uninitialized())
            assertNull(captor.value.diff)

            assertEquals(2, diffPredicateCalls)
        }

    }

})
