package ianmorgan.docstore

import com.natpryce.hamkrest.assertion.assert
import com.natpryce.hamkrest.equalTo
import graphql.GraphQL
import ianmorgan.docstore.dal.DocsDao
import ianmorgan.docstore.graphql.GraphQLFactory
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.xdescribe
import org.junit.platform.runner.JUnitPlatform
import org.junit.runner.RunWith
import java.io.FileInputStream

/**
 * A collection of tests around the problems of embedded docs, i.e. types that
 * can only accessed as part of a large aggregate,  basically the case where a Type
 * defined in the schema DOESNOT have an ID field.
 *
 */
@RunWith(JUnitPlatform::class)
object EmbeddedDocsSpec : Spek({

    val beatlesSchema = FileInputStream("src/schema/beatles.graphqls").bufferedReader().use { it.readText() }
    lateinit var docsDao: DocsDao
    lateinit var graphQL : GraphQL

    xdescribe ("GraphQL queries over the embedded docs") {

        beforeGroup {
            // setup GraphQL & DAO with some initial data
            // see https://github.com/graphql/graphql-js/blob/master/src/__tests__/starWarsData.js
            docsDao = DocsDao(beatlesSchema)
            val dataLoader = DataLoader(docsDao)
            dataLoader.loadDirectory("src/test/resources/beatles")

            graphQL = GraphQLFactory.build(beatlesSchema, docsDao)

        }

        it ("should query for a Droid by id") {

            val query = """{
                    droid(id: "2001") {name,appearsIn,primaryFunction,friends { name }}}
"""
            val result = graphQL.execute(query)

            assert.that(result.errors.isEmpty(), equalTo(true))
            assert.that(result.getData<Any>().toString(),
                equalTo("{droid={name=R2-D2, appearsIn=[NEWHOPE, EMPIRE, JEDI], primaryFunction=Astromech, friends=[{name=Luke Skywalker}]}}"))
        }

    }
})