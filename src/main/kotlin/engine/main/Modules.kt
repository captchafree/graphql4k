package engine.main

import graphql.schema.DataFetcher
import graphql.schema.DataFetchingEnvironment
import kotlin.reflect.KFunction
import kotlin.reflect.full.functions
import kotlin.reflect.full.starProjectedType
import kotlin.reflect.jvm.isAccessible
import kotlin.reflect.jvm.jvmErasure


abstract class GraphQL4KModule {

    fun install(blueprintRegistry: GraphQLBlueprintRegistry) {
        installModule(blueprintRegistry)
    }

    abstract fun installModule(blueprintRegistry: GraphQLBlueprintRegistry)
}


abstract class GraphQLModule : GraphQL4KModule() {
    override fun installModule(blueprintRegistry: GraphQLBlueprintRegistry) {
        val module = createModule()
        blueprintRegistry.mappings[this] = module
        module.installModule(blueprintRegistry)
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

        func.isAccessible = true

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

        val module = GraphQLTypeBuilderModule(typeName) {
            functions.forEach { (name, df) ->
                // Accurate field definitions only work when using the `type()` function
                // Source code location lookup doesn't work for GraphQLTypeModules yet. Any datafetcher defined
                // in one will direct you here.
                field(name, df)
            }
        }

        blueprintRegistry.mappings[this] = module
        module.installModule(blueprintRegistry)
    }
}


abstract class AbstractGraphQLModule<T : GraphQLBuilderBase>(
    private val init: T.() -> Unit
) : GraphQL4KModule() {

    override fun installModule(blueprintRegistry: GraphQLBlueprintRegistry) {
        val module = buildTarget(blueprintRegistry)
        module.apply(init)
    }

    protected abstract fun buildTarget(blueprintRegistry: GraphQLBlueprintRegistry): T
}

class GraphQLBuilderModule(
    private val init: GraphQLWiringBuilder.() -> Unit
) : AbstractGraphQLModule<GraphQLWiringBuilder>(init) {

    override fun installModule(blueprintRegistry: GraphQLBlueprintRegistry) {
        val module = buildTarget(blueprintRegistry)
        module.apply(init)
        blueprintRegistry.mappings[this] = module
    }

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
