package ianmorgan.graphstore.dal


import graphql.schema.idl.TypeDefinitionRegistry
import ianmorgan.graphstore.checker.MapChecker
import ianmorgan.graphstore.checker.SchemaBuilder
import ianmorgan.graphstore.checker.ValidatorMode
import ianmorgan.graphstore.graphql.Helper
import java.util.*
import kotlin.collections.HashSet
import kotlin.reflect.KFunction2

/**
 * A Dao for saving (as events) and retrieving a single document. The document structure
 * is controlled by the GraphQL schema.
 */
@Suppress("UNCHECKED_CAST")
class DocDao constructor(
    typeDefinitionRegistry: TypeDefinitionRegistry,
    docType: String,
    eventStoreClient: EventStoreClient = InMemoryEventStore()
) : ReaderDao, FinderDao {



    private val docType = docType
    private val es = eventStoreClient
    private val mapSchema = SchemaBuilder(typeDefinitionRegistry).build(docType)
    private lateinit var aggregateKey: String


    init {
        initAggregateKey(typeDefinitionRegistry)
    }


    /**
     * Store a document. The 'doc' is a simple map of key - value pairs and must match
     * the structure in the schema.
     *
     * See https://ianmorgan.github.io/doc-store/storage for more detail.
     */
    fun store(doc: Map<String, Any?>, validatorMode: ValidatorMode = ValidatorMode.Create) {
        val id = doc.get(aggregateKey)
        if (id is String) {
            checkAgainstSchema(doc, validatorMode)
            es.storeEvent(buildUpdateEvent(id, doc))
        } else {
            throw RuntimeException("must have an aggregate id")
        }
    }

    override fun retrieve(aggregateId: String): Map<String, Any>? {
        return DocReducer.reduceEvents(docType, es.events(aggregateId))
    }

    fun count(): Int {
        return es.aggregateKeys(docType).size
    }

    /**
     * Find all docs that match on this field name
     *
     */
    fun findByField(fieldNameExpression: String, value: Any): List<Map<String, Any>> {
        val result = ArrayList<Map<String, Any>>()

        val matcher = pickMatcher(fieldNameExpression)
        val fieldName = rootFieldName(fieldNameExpression)

        // TODO - production quality would need an indexing strategy
        for (key in es.aggregateKeys(docType)) {
            val doc = retrieve(key)!!
            if (matcher(doc[fieldName], value)) {
                result.add(doc)
            }
        }
        return result
    }

    override fun findByFields(args: Map<String, Any>): List<FindResult> {

        val result = ArrayList<FindResult>()

        // TODO - production quality would need an indexing strategy
        for (key in es.aggregateKeys(docType)) {
            val doc = retrieve(key)!!

            var matched = true
            for (arg in args.entries){
                val matcher = pickMatcher(arg.key)
                val fieldName = rootFieldName(arg.key)
                matched = matched && matcher(doc[fieldName],arg.value)

            }
            if (matched) result.add (FindResult(docType,doc["id"] as String))
        }
        return result
    }


    fun delete(aggregateId: String) {
        es.storeEvent(buildDeleteEvent(aggregateId))
    }

    /**
     * Expose the field name to be used as the aggregate key. This is
     * the "ID" field in GraphQL.
     */
    override fun aggregateKey(): String {
        return aggregateKey
    }

    private fun equalsMatcher(actual: Any?, expected: Any): Boolean {
        if (actual is String && expected is String){
            return actual.equals(expected,true)
        }
        return actual == expected
    }

    private fun containsMatcher(actual: Any?, expected: Any): Boolean {
        if (actual is String) {
            return actual.toLowerCase().contains((expected as String).toLowerCase())
        }
        if (actual is Collection<*> && expected is Collection<*>) {
            val set1 = HashSet<Any>(actual)
            val set2 = HashSet<Any>(expected)
            return set1.containsAll(set2)
        }
        if (actual is Collection<*>) {
            val set1 = HashSet<Any>(actual)
            return set1.contains(expected)
        }
        return actual.toString().toLowerCase().contains((expected as String).toLowerCase())

    }

    private fun rootFieldName(fieldNameExpression: String): String {
        return fieldNameExpression.split("_")[0]
    }

    private fun pickMatcher(fieldNameExpression: String): KFunction2<@ParameterName(name = "actual") Any?, @ParameterName(
        name = "expected"
    ) Any, Boolean> {
        if (fieldNameExpression.endsWith("_contains")) {
            return ::containsMatcher
        } else {
            return ::equalsMatcher
        }
    }

    private fun checkAgainstSchema(doc: Map<String, Any?>, validatorMode: ValidatorMode) {
        // simple implementation for now

        // TODO - make MapChecker support validator mode fully
        if (validatorMode == ValidatorMode.Create) {

            val validations = MapChecker(mapSchema).validate(doc)

            if (!validations.isEmpty()) {
                val messages = validations.joinToString(separator = "\n")
                println(messages)
                throw RuntimeException("Failed validation!\n $messages")
            }
        }

    }




    private fun initAggregateKey(registry: TypeDefinitionRegistry) {
        val helper = Helper.build(registry, docType)
        if (helper.idFieldName() != null) {
            aggregateKey = helper.idFieldName()!!
        } else {
            throw RuntimeException("Cannot find an ID field to use as the aggregateId")
        }
    }




    private fun buildUpdateEvent(aggregateId: String, data: Map<String, Any?>): Map<String, Any> {
        val ev = HashMap<String, Any>()
        ev["type"] = docType + "Updated"
        ev["id"] = UUID.randomUUID().toString()
        ev["aggregateId"] = aggregateId
        ev["timestamp"] = System.currentTimeMillis()
        ev["creator"] = "graph-store"
        ev["payload"] = data
        return ev
    }

    private fun buildDeleteEvent(aggregateId: String): Map<String, Any> {
        val ev = HashMap<String, Any>()
        ev["type"] = docType + "Deleted"
        ev["id"] = UUID.randomUUID().toString()
        ev["aggregateId"] = aggregateId
        ev["timestamp"] = System.currentTimeMillis()
        ev["creator"] = "graph-store"
        return ev
    }
}

class MapHolder constructor(theMap: Map<String, Any>) {
    private val map = theMap;

    fun theMap(): Map<String, Any> {
        return map
    }
}

