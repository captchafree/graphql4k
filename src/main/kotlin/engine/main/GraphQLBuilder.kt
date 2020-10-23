package engine.main

import graphql.ExecutionInput
import graphql.GraphQL
import graphql.execution.instrumentation.ChainedInstrumentation
import graphql.schema.GraphQLSchema
import graphql.schema.idl.SchemaGenerator
import org.dataloader.DataLoaderRegistry


fun GraphQL4KModule.buildElementRegistry(
    instanceFactory: InstanceFactory = SimpleInstanceFactory(),
    plugins: List<GraphQLBuilderPlugin> = emptyList()
): GraphQLWiringElementRegistry {
    val receiver = this

    return GraphQLWiringBuilder(GraphQLBlueprintRegistry()).apply {
        setInstanceFactory(instanceFactory)
        install(receiver)
    }.createGraphQLKit().also { registry ->
        plugins.forEach { plugin ->
            plugin.onBuild(registry)
        }
    }
}


fun GraphQL4KModule.buildSchema(
    options: GraphQLBuildingOptions = GraphQLBuildingOptions()
): GraphQLSchema {
    val (instanceFactory, schemaParser, plugins) = options

    val elementRegistry = buildElementRegistry(instanceFactory, plugins)
    val wiring = WiringBuilderVisitor().visit(elementRegistry)

    return SchemaGenerator().makeExecutableSchema(schemaParser.parse(), wiring.build())
}


fun GraphQL4KModule.buildSchema(
    elementRegistry: GraphQLWiringElementRegistry,
    schemaParser: GraphQLSchemaParser,
    plugins: List<GraphQLBuilderPlugin> = emptyList()
): GraphQLSchema {
    val wiring = WiringBuilderVisitor().visit(elementRegistry).build()

    val schema = schemaParser.parse().apply {
        plugins.forEach {
            it.transformTypeDefinitionRegistry(this)
        }
    }

    return SchemaGenerator().makeExecutableSchema(schema, wiring).apply {
        plugins.forEach {
            it.transformSchema(this)
        }
    }
}


fun GraphQL4KModule.toGraphQLKit(
    options: GraphQLBuildingOptions = GraphQLBuildingOptions()
): GraphQLKit {
    val (instanceFactory, schemaParser, plugins) = options

    val elementRegistry = buildElementRegistry(instanceFactory, plugins)
    val schema = this.buildSchema(elementRegistry, schemaParser)

    val instrumentations = elementRegistry.filterIsInstance<GraphQLInstrumentationWiringElement>().map { it.instrumentation }

    val dataLoaderBlueprints = DataLoaderRegistryVisitor().visit(elementRegistry)

    val gql = GraphQL
        .newGraphQL(schema)
        .instrumentation(ChainedInstrumentation(instrumentations))

    val executionInputProvider = object : ExecutionInputFactory {
        override fun newExecutionInput(): ExecutionInput.Builder {
            val dlRegistry = DataLoaderRegistry()
            dataLoaderBlueprints.forEach {
                dlRegistry.register(it.key, it.get())
            }

            return ExecutionInput.newExecutionInput()
                .dataLoaderRegistry(dlRegistry)
        }
    }

    return GraphQLKit(gql, executionInputProvider)
}

class DataLoaderRegistryVisitor : GraphQLWiringDefinitionVisitor<List<DataLoaderBlueprint>> {

    private val dataLoaders = mutableListOf<DataLoaderBlueprint>()

    override fun visit(elements: Collection<GraphQLWiringElement>): List<DataLoaderBlueprint> {
        dataLoaders.clear()

        elements.forEach { it.accept(this) }

        return dataLoaders
    }

    override fun visit(element: GraphQLDataLoaderWiringElement<*, *>) {
        val (_, key, createForEachRequest, supplier) = element

        val dataLoader = if (createForEachRequest) {
            DataLoaderBlueprint.OnDemandDataLoader(key, supplier)
        } else {
            DataLoaderBlueprint.PersistentDataLoader(key, supplier.get())
        }

        this.dataLoaders.add(dataLoader)
    }
}


data class GraphQLBuildingOptions(
    val instanceFactory: InstanceFactory = SimpleInstanceFactory(),
    val schemaParser: GraphQLSchemaParser = ResourcesSchemaParser(),
    val plugins: List<GraphQLBuilderPlugin> = emptyList()
) {

    companion object {
        fun newOptions(): Builder {
            return Builder()
        }
    }

    class Builder {

        private var instanceFactory: InstanceFactory = SimpleInstanceFactory()
        private var schemaParser: GraphQLSchemaParser = ResourcesSchemaParser()
        private var plugins: MutableList<GraphQLBuilderPlugin> = mutableListOf()


        fun instanceFactory(instanceFactory: InstanceFactory) = apply {
            this.instanceFactory = instanceFactory
        }

        fun schemaParser(schemaParser: GraphQLSchemaParser) = apply {
            this.schemaParser = schemaParser
        }

        fun withPlugin(plugin: GraphQLBuilderPlugin) = apply {
            this.plugins.add(plugin)
        }


        fun build(): GraphQLBuildingOptions {
            return GraphQLBuildingOptions(instanceFactory, schemaParser, plugins)
        }
    }
}