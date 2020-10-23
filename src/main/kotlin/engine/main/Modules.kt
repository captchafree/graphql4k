package engine.main

import graphql.schema.DataFetcher
import graphql.schema.DataFetchingEnvironment
import kotlin.reflect.KFunction
import kotlin.reflect.full.functions
import kotlin.reflect.full.starProjectedType
import kotlin.reflect.jvm.jvmErasure


interface GraphQL4KModule {
    fun install(blueprintRegistry: GraphQLBlueprintRegistry)
}


abstract class GraphQLModule : GraphQL4KModule {
    override fun install(blueprintRegistry: GraphQLBlueprintRegistry) {
        createModule().install(blueprintRegistry)
    }

    abstract fun createModule(): AbstractGraphQLModule<*>
}


@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
annotation class GraphQLDataFetcher(val value: String = "")

abstract class GraphQLTypeModule {

    private fun getDataFetcherName(func: KFunction<*>): String {
        val annotation = (func.annotations.firstOrNull { it is GraphQLDataFetcher } as GraphQLDataFetcher?)

        return when {
            annotation == null || annotation.value == "" -> func.name
            else -> annotation.value
        }
    }

    private fun parseFunction(func: KFunction<*>): Pair<String, DataFetcher<*>> {

        // Function returns a datafetcher
        if (DataFetcher::class.java.isAssignableFrom(func.returnType.jvmErasure.java)) {
            check (func.parameters.size == 1) {
                "A function returning a DataFetcher must contain no parameters"
            }

            return Pair(getDataFetcherName(func), func.call(this) as DataFetcher<*>)
        } else {
            check (func.parameters.size <= 2) {
                "A function acting as a DataFetcher can only have no args or a single arg of type `DataFetchingEnvironment`"
            }

            val hasEnvArg = func.parameters.firstOrNull {
                it.type == DataFetchingEnvironment::class.starProjectedType
            } != null

            val df = DataFetcher {
                if (hasEnvArg) {
                    func.call(this, it)
                } else {
                    func.call(this)
                }
            }

            return Pair(getDataFetcherName(func), df)
        }
    }

    private fun getDataFetcherMethods(): Collection<Pair<String, DataFetcher<*>>> {
        return this::class.functions.filter {
            (it.annotations.firstOrNull { it is GraphQLDataFetcher } as GraphQLDataFetcher?) != null
        }.map {
            parseFunction(it)
        }
    }

    fun install(typeName: String, blueprintRegistry: GraphQLBlueprintRegistry) {
        val functions = getDataFetcherMethods()

        GraphQLKit.typeModule(typeName) {
            functions.forEach { (name, df) ->
                // If you are looking for a field definition and got directed here I have bad news...
                // Source code location lookup doesn't work for GraphQLTypeModules yet. Any datafetcher defined
                // in one will direct you here.
                field(name, df)
            }
        }.install(blueprintRegistry)
    }
}


abstract class AbstractGraphQLModule<T : GraphQLBuilderBase>(
    private val init: T.() -> Unit
) : GraphQL4KModule {

    override fun install(blueprintRegistry: GraphQLBlueprintRegistry) {
        buildTarget(blueprintRegistry).apply(init)
    }

    protected abstract fun buildTarget(context: GraphQLBlueprintRegistry): T
}

class GraphQLBuilderModule(
    init: GraphQLWiringBuilder.() -> Unit
) : AbstractGraphQLModule<GraphQLWiringBuilder>(init) {

    override fun buildTarget(blueprintRegistry: GraphQLBlueprintRegistry): GraphQLWiringBuilder {
        return GraphQLWiringBuilder(blueprintRegistry)
    }
}

class GraphQLTypeBuilderModule(
    private val typeName: String,
    init: GraphQLTypeBuilder.() -> Unit
) : AbstractGraphQLModule<GraphQLTypeBuilder>(init) {

    override fun buildTarget(blueprintRegistry: GraphQLBlueprintRegistry): GraphQLTypeBuilder {
        return GraphQLTypeBuilder(blueprintRegistry, typeName)
    }
}
