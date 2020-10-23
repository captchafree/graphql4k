import engine.ext.CachedPreparsedDocumentProvider
import engine.ext.CostInstrumentation
import engine.ext.PrettyPrintSchemaPlugin
import engine.main.GraphQLBuildingOptions
import engine.main.GraphQLKit
import engine.main.GraphQLKit.Companion.buildGraphQLKit
import engine.main.GraphQLKit.Companion.graphQLModule
import engine.main.GraphQLModule
import org.dataloader.BatchLoader
import java.util.concurrent.CompletableFuture.supplyAsync


class SampleModule : GraphQLModule() {
    override fun createModule() = graphQLModule {
        query {
            field("test") { "success!" }
        }
    }
}

fun main() {

    val queryModule = GraphQLKit.queryModule {
        field("pong") { "ping" }

        field("ping") { env ->
            env.getDataLoader<String, String>("ping").load(env.field.name)
        }

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

        instrumentation(CostInstrumentation(3))

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
        .preparsedDocumentProvider(CachedPreparsedDocumentProvider())
        .build()

    val input = eiFactory
        .newExecutionInput()
        .query("query { ping\ntest\nvalue { id\nname } }")

    graphql.execute(input).toSpecification().also { println(it) }
}
