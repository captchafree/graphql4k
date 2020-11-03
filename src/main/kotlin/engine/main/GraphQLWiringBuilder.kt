package engine.main

import graphql.execution.instrumentation.Instrumentation
import graphql.schema.GraphQLScalarType
import graphql.schema.TypeResolver
import graphql.schema.idl.SchemaDirectiveWiring
import org.dataloader.BatchLoader
import org.dataloader.DataLoader
import org.dataloader.MappedBatchLoader
import java.util.function.Supplier
import kotlin.reflect.KClass


@DslMarker
annotation class GraphQLBuilderMarker

@GraphQLBuilderMarker
abstract class GraphQLBuilderBase(
    protected val blueprintRegistry: GraphQLBlueprintRegistry
) {

    inline fun <reified T : Any> lazyInject(): Lazy<T> = lazy {
        this.getInstance()
    }

    inline fun <reified T : Any> getInstance(): T {
        return getInstance(T::class)
    }

    fun <T : Any> getInstance(clazz: KClass<T>): T {
        return blueprintRegistry.instanceFactory.getInstance(clazz)
    }

    /**
     * Returns the stack trace element `depth` items up the stack from where
     * the method is invoked
     */
    protected fun getCallerCodePosition(depth: Int = 5): StackTraceElement {
        return Thread.currentThread().stackTrace[depth-1]
    }
}


class GraphQLWiringBuilder(blueprintRegistry: GraphQLBlueprintRegistry) : GraphQLBuilderBase(blueprintRegistry) {

    fun setInstanceFactory(instanceFactory: InstanceFactory) {
        blueprintRegistry.instanceFactory = instanceFactory
    }


    inline fun <reified T : GraphQL4KModule> install() {
        install(getInstance<T>())
    }

    fun install(module: GraphQL4KModule) {
        blueprintRegistry.addDefinition(GraphQLWiringModuleElement(getCallerCodePosition(), module))
        module.install(blueprintRegistry)
    }


    inline fun <reified T : GraphQLTypeModule> bindType(typeName: String) {
        bindType(typeName, getInstance<T>())
    }

    fun <T : GraphQLTypeModule> bindType(typeName: String, module: T) {
        blueprintRegistry.addDefinition(GraphQLTypeBindingWiringElement(getCallerCodePosition(), typeName, module))
        module.install(typeName, blueprintRegistry)
    }


    fun type(typeName: String, init: GraphQLTypeBuilder.() -> Unit) {
        blueprintRegistry.addDefinition(GraphQLTypeWiringElement(getCallerCodePosition(), typeName))
        GraphQLTypeBuilder(blueprintRegistry, typeName).init()
    }

    fun query(init: GraphQLTypeBuilder.() -> Unit) {
        type("Query", init)
    }

    fun mutation(init: GraphQLTypeBuilder.() -> Unit) {
        type("Mutation", init)
    }

    fun subscription(init: GraphQLTypeBuilder.() -> Unit) {
        type("Subscription", init)
    }


    fun <I, R> dataLoader(key: String, createForEachRequest: Boolean = false, supplier: Supplier<DataLoader<I, R>>) {
        blueprintRegistry.addDefinition(GraphQLDataLoaderWiringElement(getCallerCodePosition(7), key, createForEachRequest, supplier))
    }

    fun <I, R> batchLoader(key: String, createForEachRequest: Boolean = false, supplier: Supplier<BatchLoader<I, R>>) {
        dataLoader(key, createForEachRequest) { DataLoader.newDataLoader(supplier.get()) }
    }

    fun <I, R> mappedBatchLoader(key: String, createForEachRequest: Boolean = false, supplier: Supplier<MappedBatchLoader<I, R>>) {
        dataLoader(key, createForEachRequest) { DataLoader.newMappedDataLoader(supplier.get()) }
    }


    fun scalar(scalar: GraphQLScalarType) {
        blueprintRegistry.addDefinition(GraphQLScalarWiringElement(getCallerCodePosition(), scalar))
    }

    fun directive(name: String, schemaDirectiveWiring: SchemaDirectiveWiring) {
        blueprintRegistry.addDefinition(GraphQLDirectiveWiringElement(getCallerCodePosition(), name, schemaDirectiveWiring))
    }

    fun instrumentation(instrumentation: Instrumentation) {
        blueprintRegistry.addDefinition(GraphQLInstrumentationWiringElement(getCallerCodePosition(), instrumentation))
    }

    fun typeResolver(typeName: String, typeResolver: TypeResolver) {
        blueprintRegistry.addDefinition(GraphQLTypeResolverWiringElement(getCallerCodePosition(), typeName, typeResolver))
    }


    fun createGraphQLKit(): GraphQLWiringElementRegistry {

        blueprintRegistry.mappings.forEach { t, u ->
            println("$t: $u")
        }

        return List(blueprintRegistry.elements.size) {
            blueprintRegistry.elements[it]
        }
    }
}



class SchemaPrintingVisitor : GraphQLWiringDefinitionVisitor<Unit> {

    private val fields = mutableListOf<GraphQLFieldWiringElement>()

    override fun visit(elements: Collection<GraphQLWiringElement>) {
        fields.clear()
        elements.forEach { it.accept(this) }

        fields.sortBy { it.fieldName }
        val results = fields.groupBy { it.typeName }

        val sb = StringBuilder().apply {
            for ((typeName, fields) in results) {
                appendLine("type $typeName {")

                val groupedFields = fields.groupBy { it.fieldName }

                for ((name, fields) in groupedFields) {
                    appendLine("\t${name}")

                    for (field in fields) {
                        appendLine("\t \u221f${field.sourceLocation}")
                    }
                }
                appendLine("}\n")
            }
        }

        println(sb)
    }

    override fun visit(element: GraphQLFieldWiringElement) {
        fields.add(element)
    }
}

