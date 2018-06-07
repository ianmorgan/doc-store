package ianmorgan.docstore.graphql

import graphql.language.ObjectTypeDefinition
import graphql.schema.DataFetcher
import graphql.schema.DataFetchingEnvironment
import ianmorgan.docstore.DocsDao
import java.util.HashMap

/**
 * A DataFetcher for a single doc, linked to its DAO. This fetcher is passed the
 * complete ObjectTypeDefinition and also knows how to resolve data for child nodes, which requires
 * recursive calls to the DAOs.
 */
class DocDataFetcher constructor(docsDao: DocsDao, docName: String, typeDefinition : ObjectTypeDefinition) :
    DataFetcher<Map<String, Any>?> {
    val dao = docsDao
    val docName = docName
    val typeDefinition = typeDefinition
    override fun get(env: DataFetchingEnvironment): Map<String, Any>? {

        val idFieldName = dao.daoForDoc(docName).aggregateKey()
        val id = env.getArgument<String>(idFieldName)
        val data = lookupById(id)

        if (data != null) {
            val helper = Helper.build(typeDefinition)
            for (f in helper.listTypeFieldNames()) {
                val typeName = helper.typeForField(f)

                if (typeName == docName) {
                    val ids = data.getOrDefault(f, emptyList<String>()) as List<String>
                    val expanded = ArrayList<Map<String, Any>>()
                    for (theId in ids) {
                        val x = lookupById(theId)
                        if (x != null) {
                            expanded.add(x)
                        }
                    }
                    data.put(f, expanded)
                }
            }
        }
        return data
    }

    private fun lookupById (id : String) : HashMap<String, Any>? {
        val data = dao.daoForDoc(docName).retrieve(id)
        if (data != null){
            return HashMap(data);
        }
        else {
            return null;
        }
    }
}

/**
 * A DataFetcher for a single doc, linked to its DAO. This fetcher is passed the
 * complete ObjectTypeDefinition and also knows how to resolve data for child nodes, which requires
 * recursive calls to the DAOs.
 */
class DocListDataFetcher constructor(docsDao: DocsDao, docName: String, typeDefinition : ObjectTypeDefinition) :
    DataFetcher<List<Map<String, Any>?>> {
    val dao = docsDao
    val docName = docName
    val typeDefinition = typeDefinition
    override fun get(env: DataFetchingEnvironment): List<Map<String, Any>?> {

        if (env.containsArgument("name")){
            val name = env.getArgument<String>("name")
            return  dao.daoForDoc(docName).findByField("name",name);
        }

        return emptyList()
    }
}


/**
 * A DataFetcher that just tries all docs. The most basic way of dealing with interfaces
 */
class DocsDataFetcher constructor(docsDao: DocsDao) : DataFetcher<Map<String, Any>?> {
    val daos = docsDao
    override fun get(env: DataFetchingEnvironment): Map<String, Any>? {
        val id = env.getArgument<String>("id")

        for (doc in daos.availableDocs()) {
            val data = daos.daoForDoc(doc).retrieve(id)
            if (data != null) {
                return data
            }
        }
        return null;
    }
}


/**
 * Does nothing - useful for experimenting and debugging only
 */
class NullDataFetcher : DataFetcher<Map<String, Any>?> {
    override fun get(environment: DataFetchingEnvironment?): Map<String, Any>? {
        println("In NullDataFetcher ")
        return emptyMap()
    }
}

object Fetcher {
    fun docFetcher (docsDao: DocsDao, docName: String, typeDefinition : ObjectTypeDefinition) : DataFetcher<Map<String, Any>?> {
        return DocDataFetcher(docsDao, docName,typeDefinition)
    }

    fun interfaceFetcher (docsDao: DocsDao,  typeDefinition : ObjectTypeDefinition?) : DataFetcher<Map<String, Any>?> {
        return DocsDataFetcher(docsDao)
    }

    fun docListFetcher (docsDao: DocsDao, docName: String, typeDefinition : ObjectTypeDefinition) : DataFetcher<List<Map<String, Any>?>> {
        return DocListDataFetcher(docsDao, docName,typeDefinition)
    }

    fun nullDocFetcher() : DataFetcher<Map<String, Any>?> {
        return NullDataFetcher()
    }
}