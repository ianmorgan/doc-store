package docstore.ianmorgan.github.io

import com.natpryce.hamkrest.assertion.assert
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.throws
import graphql.language.ObjectTypeDefinition
import graphql.schema.idl.SchemaParser
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import org.junit.platform.runner.JUnitPlatform
import org.junit.runner.RunWith
import kotlin.reflect.KClass

@RunWith(JUnitPlatform::class)
object DocDaoSpec : Spek({

    val schema = """
enum Episode {
  NEWHOPE
  EMPIRE
  JEDI
}

type Droid  {
  id: ID!
  name: String!
  friends: [Character]
  appearsIn: [Episode]!
  primaryFunction: String
}
"""

    lateinit var type: ObjectTypeDefinition

    describe ("A DAO for a document") {
        beforeGroup {
            val schemaParser = SchemaParser()
            val typeDefinitionRegistry = schemaParser.parse(schema)
            type = typeDefinitionRegistry.getType("Droid", ObjectTypeDefinition::class.java).get()
        }

        it ("should use the 'ID' field as the aggregate id"){
            val dao = DocDao(type)
            assert.that(dao.aggregateKey(), equalTo("id"))
        }

        it ("should throw exception if there is no 'ID' field"){
            val registry  = SchemaParser().parse("type Droid { name: String!} ")
            val type = registry.getType("Droid", ObjectTypeDefinition::class.java).get()

            assert.that({DocDao(type)}, throws<RuntimeException>())
        }

        it ("should build the 'fields' collection from the GraphQL schema"){
            val dao = DocDao(type)
            assert.that(dao.fields().size, equalTo(1))
            assert.that(dao.fields().get("name"), equalTo(String::class as KClass<Any>))
        }

    }

})