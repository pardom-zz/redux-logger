package redux.logger

import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import org.junit.platform.runner.JUnitPlatform
import org.junit.runner.RunWith
import redux.logger.Diff.Change
import redux.logger.Diff.Change.Modification
import java.util.Date

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

    data class Box<out T : Any>(
        val value: T
    )

    data class GenericTodo(
        val title: String,
        val done: Box<Box<Boolean>>
    )

    data class MapTodo(
        val title: String,
        val properties: Map<String, Any?>
    )

    describe("Diff") {

        describe("calculate") {

            it("detects modifications") {
                val now = System.currentTimeMillis()
                val then = now + 1000 * 60 * 60 * 3
                val todo1 = DatedTodo("Get milk", Date(now), false)
                val todo2 = DatedTodo("Get milk", Date(then), true)

                val changes = Diff.calculate(todo1, todo2)
                val expected = listOf(
                    Change.Modification("date", Date(now), Date(then)),
                    Change.Modification("done", false, true)
                )

                assertThat(changes).hasSameElementsAs(expected)
            }

            it("detects additions") {
                val now = System.currentTimeMillis()
                val then = now + 1000 * 60 * 60 * 3
                val todo1 = Todo("Get milk", false)
                val todo2 = DatedTodo("Get milk", Date(then), true)

                val changes = Diff.calculate(todo1, todo2)
                val expected = listOf(
                    Change.Addition("date", Date(then)),
                    Change.Modification("done", false, true)
                )

                assertThat(changes).hasSameElementsAs(expected)
            }

            it("detects deletions") {
                val now = System.currentTimeMillis()
                val todo1 = DatedTodo("Get milk", Date(now), false)
                val todo2 = Todo("Get milk", true)

                val changes = Diff.calculate(todo1, todo2)
                val expected = listOf(
                    Change.Deletion("date", Date(now)),
                    Change.Modification("done", false, true)
                )

                assertThat(changes).hasSameElementsAs(expected)
            }

            it("handles recursive modifications") {
                val amanda = User("Amanda")
                val todo1 = AssignedTodo("Get milk", false, User("Michael"))
                val todo2 = AssignedTodo("Get milk", false, User("Michael", amanda))

                val changes = Diff.calculate(todo1, todo2)
                val expected = listOf(
                    Change.Modification(
                        "user.director",
                        null,
                        mapOf(
                            "user.director.name" to "Amanda",
                            "user.director.director" to null
                        )
                    )
                )

                assertThat(changes).hasSameElementsAs(expected)
            }

            it("handles generics") {
                val todo1 = GenericTodo("Get milk", Box(Box(false)))
                val todo2 = GenericTodo("Get milk", Box(Box(true)))

                val changes = Diff.calculate(todo1, todo2)
                val expected = listOf(
                    Modification("done.value.value", todo1.done.value.value, todo2.done.value.value)
                )

                assertThat(changes).hasSameElementsAs(expected)
            }

            it("does not conflict with input maps") {
                val now = System.currentTimeMillis()
                val then = now + 1000 * 60 * 60 * 3
                val todo1 = MapTodo("Get milk", mapOf(
                    "date" to Date(now),
                    "done" to false
                ))
                val todo2 = MapTodo("Get milk", mapOf(
                    "date" to Date(then),
                    "done" to true
                ))

                val changes = Diff.calculate(todo1, todo2)
                val expected = listOf(
                    Change.Modification("date", Date(now), Date(then)),
                    Change.Modification("done", false, true)
                )

                assertThat(changes).hasSameElementsAs(expected)
            }

        }

    }

})
