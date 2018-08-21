package ianmorgan.graphstore

import com.natpryce.hamkrest.assertion.assert
import com.natpryce.hamkrest.equalTo
import ianmorgan.graphstore.graphql.ArgsWalker
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import org.junit.platform.runner.JUnitPlatform
import org.junit.runner.RunWith

@RunWith(JUnitPlatform::class)
object ArgsWalkerSpec : Spek({

    val example = mapOf(
        "name" to emptyMap<String, Any>(),
        "friends" to mapOf("length" to 10),
        "friends/name" to emptyMap()
    )

    describe("walking the query args") {

        it("should walk simple args") {

            val root = ArgsWalker("/", example)
            val friends = root.walkPath("friends")

            assert.that(friends.args(), equalTo(mapOf("name" to emptyMap<String, Any>())))
            assert.that(friends.path(), equalTo("friends"))
            assert.that(friends.parent(), equalTo(root))
        }

        it("should build root node") {
            val root = ArgsWalker("/", example)

            assert.that(root.args(), equalTo(example))
            assert.that(root.path(), equalTo("/"))

            // how to do a null check using hamcrest
            //assert.that(root.parent(), nothing)
        }

    }

})
