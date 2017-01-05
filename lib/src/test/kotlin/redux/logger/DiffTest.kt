package redux.logger

import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import org.junit.platform.runner.JUnitPlatform
import org.junit.runner.RunWith
import redux.logger.Diff.Change
import java.util.Date
import kotlin.test.expect

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
class DiffTest : Spek({

    data class User(
        val name: String,
        val director: User? = null
    )

    data class Todo(
        val title: String,
        val done: Boolean
    )

    data class DatedTodo(
        val title: String,
        val date: Date,
        val done: Boolean
    )

    data class AssignedTodo(
        val title: String,
        val done: Boolean,
        val user: User
    )

    describe("Diff") {

        describe("calculate") {

            it("detects modifications") {
                val now = System.currentTimeMillis()
                val then = now + 1000 * 60 * 60 * 3
                val todo1 = DatedTodo("Get milk", Date(now), false)
                val todo2 = DatedTodo("Get milk", Date(then), true)

                val changes = Diff.calculate(todo1, todo2)

                expect(
                    listOf(
                        Change.Modification("date", Date(now), Date(then)),
                        Change.Modification("done", false, true)
                    )
                ) { changes }
            }

            it("detects additions") {
                val now = System.currentTimeMillis()
                val then = now + 1000 * 60 * 60 * 3
                val todo1 = Todo("Get milk", false)
                val todo2 = DatedTodo("Get milk", Date(then), true)

                val changes = Diff.calculate(todo1, todo2)

                expect(
                    listOf(
                        Change.Addition("date", Date(then)),
                        Change.Modification("done", false, true)
                    )
                ) { changes }
            }

            it("detects deletions") {
                val now = System.currentTimeMillis()
                val todo1 = DatedTodo("Get milk", Date(now), false)
                val todo2 = Todo("Get milk", true)

                val changes = Diff.calculate(todo1, todo2)

                expect(
                    listOf(
                        Change.Deletion("date", Date(now)),
                        Change.Modification("done", false, true)
                    )
                ) { changes }
            }

            it("handles recursive modifications") {
                val amanda = User("Amanda")
                val todo1 = AssignedTodo("Get milk", false, User("Michael"))
                val todo2 = AssignedTodo("Get milk", false, User("Michael", amanda))

                val changes = Diff.calculate(todo1, todo2)

                expect(
                    listOf(
                        Change.Modification(
                            "user",
                            mapOf(
                                "name" to "Michael",
                                "director" to null
                            ),
                            mapOf(
                                "name" to "Michael",
                                "director" to mapOf(
                                    "name" to "Amanda",
                                    "director" to null
                                )
                            )
                        )
                    )
                ) { changes }
            }

        }

    }

})
