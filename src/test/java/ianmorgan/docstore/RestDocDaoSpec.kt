package ianmorgan.docstore

import com.natpryce.hamkrest.assertion.assert

import com.natpryce.hamkrest.equalTo
import graphql.schema.idl.SchemaParser
import ianmorgan.docstore.dal.ConfigurableRestDocDao
import ianmorgan.docstore.dal.StarshipDocDao
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import org.junit.platform.runner.JUnitPlatform
import org.junit.runner.RunWith

@RunWith(JUnitPlatform::class)
object RestDocDaoSpec : Spek({

    describe("A REST DAO for external starwars data") {
        beforeGroup {
            val schemaParser = SchemaParser()
        }

        it("should use the 'ID' field as the aggregate id") {
            val dao = ConfigurableRestDocDao()

            val result = dao.retrieve("10")
            val expected = mapOf ("name" to "Millennium Falcon",
                "manufacturer" to "Corellian Engineering Corporation",
                "model" to "YT-1300 light freighter",
                "lengthInMetres" to "34.37",
                "costInCredits" to "100000"
                ) as Map<String,Any>

           assert.that(result, equalTo(expected))
        }
    }


})


