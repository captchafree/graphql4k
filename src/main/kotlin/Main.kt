import engine.ext.CostInstrumentation
import engine.ext.PrettyPrintSchemaPlugin
import engine.main.*
import engine.main.GraphQLKit.Companion.buildGraphQLKit
import graphql.execution.instrumentation.tracing.TracingInstrumentation
import graphql.schema.DataFetcher
import org.dataloader.BatchLoader
import java.util.concurrent.CompletableFuture.supplyAsync

object GlobalContext {
    val moduleMappings: MutableMap<Any, MutableList<Any>> = mutableMapOf()
}

class SampleModule : GraphQLModule() {
    override fun createModule() = GraphQLBuilderModule {
        query {
            field("test") { "success!" }
        }
    }
}

class TestTypeModule : GraphQLTypeModule() {

    @GraphQLDataFetcher
    private fun ping() = DataFetcher<String> {
        "pong!!!"
    }

}

fun main() {

    val queryModule = GraphQLTypeBuilderModule("Query") {
        field("pong") { "ping" }

        field("value") {
            supplyAsync {
                listOf(
                    mapOf(
                        "id" to "1",
                        "name" to "Neato!"
                    )
                )
            }
        }
    }

    val otherModule = GraphQLBuilderModule {
        install(queryModule)
    }


    val masterModule = GraphQLBuilderModule {
        install(otherModule)
        install<SampleModule>()

        bindType<TestTypeModule>("Query")

        instrumentation(CostInstrumentation(3))
        instrumentation(TracingInstrumentation())

        batchLoader<String, String>("ping") {
            BatchLoader<String, String> { keys ->
                supplyAsync {
                    List(keys.size) { "pong" }
                }
            }
        }
    }



    val options = GraphQLBuildingOptions.newOptions()
        .withPlugin(PrettyPrintSchemaPlugin())
        .build()


    val (graphqlBuilder, eiFactory) = buildGraphQLKit(options, masterModule)

    /*
    val graphql = graphqlBuilder
        .preparsedDocumentProvider(ApolloPersistedQuerySupport(InMemoryPersistedQueryCache()))
        //.preparsedDocumentProvider(CachedPreparsedDocumentProvider())
        .build()

    val ext = mapOf(
        "persistedQuery" to mapOf(
            "sha256Hash" to "1"
        )
    )

    val ext1 = mapOf(
        "persistedQuery" to mapOf(
            "sha256Hash" to "1"
        )
    )

    val input = eiFactory
        .newExecutionInput()
        .operationName("MyQuery")
        .query("query MyQuery { ping\ntest\nvalue { id\nname } }")
        .extensions(ext)

    val input1 = eiFactory
        .newExecutionInput()
        .operationName("MyQuery")
        .query("")
        .extensions(ext1)



    graphql.execute(input).toSpecification().also { println(it) }
    graphql.execute(input1).toSpecification().also { println(it) }
    graphql.execute(input1).toSpecification().also { println(it) }

    */

}
