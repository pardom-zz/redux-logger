package redux.logger

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

    fun log(event: Event, action: Any, state: S)

    enum class Event {
        DISPATCH, STATE
    }

    companion object {

        operator fun <S : Any> invoke(f: (event: Event, action: Any, state: S) -> Any?): Logger<S> {
            return object : Logger<S> {
                override fun log(event: Event, action: Any, state: S) {
                    f(event, action, state)
                }
            }
        }

    }

}
