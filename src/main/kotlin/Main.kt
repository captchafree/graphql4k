import engine.ext.CostInstrumentation
import engine.ext.InMemoryPersistedQueryCache
import engine.ext.PrettyPrintSchemaPlugin
import engine.main.*
import engine.main.GraphQLKit.Companion.buildGraphQLKit
import engine.main.GraphQLKit.Companion.graphQLModule
import graphql.execution.instrumentation.tracing.TracingInstrumentation
import graphql.execution.preparsed.persisted.ApolloPersistedQuerySupport
import graphql.schema.DataFetcher
import org.dataloader.BatchLoader
import java.util.concurrent.CompletableFuture.supplyAsync


class SampleModule : GraphQLModule() {
    override fun createModule() = graphQLModule {
        query {
            field("test") { "success!" }
        }
    }
}

class TestTypeModule : GraphQLTypeModule() {

    @GraphQLDataFetcher
    fun ping() = DataFetcher<String> {
        "pong!!!"
    }

}

fun main() {

    val queryModule = GraphQLKit.queryModule {
        field("pong") { "ping" }

        /*field("ping") { env ->
            env.getDataLoader<String, String>("ping").load(env.field.name)
        }*/

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


    val masterModule = graphQLModule {
        install(queryModule)
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

}
