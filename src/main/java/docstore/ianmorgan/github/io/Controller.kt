package docstore.ianmorgan.github.io

import graphql.GraphQL
import io.javalin.ApiBuilder
import io.javalin.Javalin

class Controller constructor(dao : DocDao, graphQL : GraphQL){
    private val theDao = dao
    private val graphQL = graphQL


    fun register(app: Javalin) {
        app.routes {
            ApiBuilder.get("/graphql") { ctx ->

                val query = ctx.queryParam("query")

                if (query != null){
                    val executionResult = graphQL.execute(query)
                    println(executionResult.getData<Any>().toString())

                    // todo - what about errors

                    val result = mapOf("data" to executionResult.getData<Any>());
                    ctx.json(result)
                }


            }


        }


    }
}
