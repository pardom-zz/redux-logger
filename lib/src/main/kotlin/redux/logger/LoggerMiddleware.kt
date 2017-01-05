package redux.logger

import redux.api.Store
import redux.api.enhancer.Middleware
import redux.logger.Logger.Entry

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

fun <S : Any> createLoggerMiddleware(
    logger: Logger<S>,
    predicate: (Any, Store<S>) -> Boolean = { action, store -> true },
    diffPredicate: (Any, Store<S>) -> Boolean = { action, store -> false }): Middleware<S> {

    return Middleware { store, next, action ->
        if (!predicate(action, store)) {
            next.dispatch(action)
        }
        else {
            val startTime = System.currentTimeMillis()
            val oldState = store.state
            val result = next.dispatch(action)
            val newState = store.state
            val endTime = System.currentTimeMillis()

            val diff = when {
                diffPredicate(action, store) -> Diff.calculate(oldState, newState)
                else -> null
            }

            logger.log(
                Entry(
                    action,
                    oldState,
                    newState,
                    startTime,
                    endTime,
                    endTime - startTime,
                    diff
                )
            )

            result
        }
    }
}
