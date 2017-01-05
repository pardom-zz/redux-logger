package redux.logger

import redux.logger.Diff.Change

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

interface Logger<in S : Any> {

    data class Entry<out S : Any>(
        val action: Any,
        val oldState: S,
        val newState: S,
        val startTime: Long,
        val endTime: Long,
        val duration: Long,
        val diff: List<Change>?
    )

    fun log(entry: Entry<S>)

    companion object {

        operator fun <S : Any> invoke(f: (entry: Entry<S>) -> Any?): Logger<S> {
            return object : Logger<S> {
                override fun log(entry: Entry<S>) {
                    f(entry)
                }
            }
        }

    }

}
