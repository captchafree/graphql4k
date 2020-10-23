package engine.main

import graphql.execution.instrumentation.Instrumentation
import graphql.schema.DataFetcher
import graphql.schema.GraphQLScalarType
import graphql.schema.TypeResolver
import graphql.schema.idl.RuntimeWiring
import graphql.schema.idl.SchemaDirectiveWiring
import graphql.schema.idl.TypeRuntimeWiring
import org.dataloader.DataLoader
import java.util.function.Supplier


typealias GraphQLWiringElementRegistry = Collection<GraphQLWiringElement>

sealed class GraphQLWiringElement(
    open val sourceLocation: StackTraceElement
) {
    open fun <T> accept(visitor: GraphQLWiringDefinitionVisitor<T>) {
        return visitor.visit(this)
    }
}

data class GraphQLWiringModuleElement(
    override val sourceLocation: StackTraceElement,
    val module: GraphQL4KModule
) : GraphQLWiringElement(sourceLocation) {
    override fun <T> accept(visitor: GraphQLWiringDefinitionVisitor<T>) {
        return visitor.visit(this)
    }
}

data class GraphQLTypeWiringElement(
    override val sourceLocation: StackTraceElement,
    val typeName: String
) : GraphQLWiringElement(sourceLocation) {
    override fun <T> accept(visitor: GraphQLWiringDefinitionVisitor<T>) {
        return visitor.visit(this)
    }
}

data class GraphQLFieldWiringElement(
    override val sourceLocation: StackTraceElement,
    val typeName: String,
    val fieldName: String,
    val dataFetcher: DataFetcher<*>
) : GraphQLWiringElement(sourceLocation)  {
    override fun <T> accept(visitor: GraphQLWiringDefinitionVisitor<T>) {
        return visitor.visit(this)
    }
}

data class GraphQLDataLoaderWiringElement<I, R>(
    override val sourceLocation: StackTraceElement,
    val key: String,
    val createForEachRequest: Boolean,
    val supplier: Supplier<DataLoader<I, R>>
) : GraphQLWiringElement(sourceLocation) {
    override fun <T> accept(visitor: GraphQLWiringDefinitionVisitor<T>) {
        return visitor.visit(this)
    }
}

data class GraphQLScalarWiringElement(
    override val sourceLocation: StackTraceElement,
    val scalar: GraphQLScalarType
) : GraphQLWiringElement(sourceLocation) {
    override fun <T> accept(visitor: GraphQLWiringDefinitionVisitor<T>) {
        return visitor.visit(this)
    }
}

data class GraphQLDirectiveWiringElement(
    override val sourceLocation: StackTraceElement,
    val name: String,
    val schemaDirectiveWiring: SchemaDirectiveWiring
) : GraphQLWiringElement(sourceLocation) {
    override fun <T> accept(visitor: GraphQLWiringDefinitionVisitor<T>) {
        return visitor.visit(this)
    }
}

data class GraphQLInstrumentationWiringElement(
    override val sourceLocation: StackTraceElement,
    val instrumentation: Instrumentation
) : GraphQLWiringElement(sourceLocation) {
    override fun <T> accept(visitor: GraphQLWiringDefinitionVisitor<T>) {
        return visitor.visit(this)
    }
}

data class GraphQLTypeResolverWiringElement(
    override val sourceLocation: StackTraceElement,
    val typeName: String,
    val typeResolver: TypeResolver
) : GraphQLWiringElement(sourceLocation) {
    override fun <T> accept(visitor: GraphQLWiringDefinitionVisitor<T>) {
        return visitor.visit(this)
    }
}


interface GraphQLWiringDefinitionVisitor<T> {

    fun visit(elements: Collection<GraphQLWiringElement>): T

    fun visit(element: GraphQLWiringElement) {}

    fun visit(element: GraphQLWiringModuleElement) {}
    fun visit(element: GraphQLTypeWiringElement) {}
    fun visit(element: GraphQLFieldWiringElement) {}
    fun visit(element: GraphQLDataLoaderWiringElement<*,*>) {}
    fun visit(element: GraphQLScalarWiringElement) {}
    fun visit(element: GraphQLDirectiveWiringElement) {}
    fun visit(element: GraphQLInstrumentationWiringElement) {}
    fun visit(element: GraphQLTypeResolverWiringElement) {}
}

class WiringBuilderVisitor : GraphQLWiringDefinitionVisitor<RuntimeWiring.Builder> {

    private val wiringBuilder = RuntimeWiring.newRuntimeWiring()

    override fun visit(elements: Collection<GraphQLWiringElement>): RuntimeWiring.Builder {
        elements.forEach {
            it.accept(this)
        }

        return wiringBuilder
    }

    override fun visit(element: GraphQLFieldWiringElement) {
        val type = TypeRuntimeWiring.newTypeWiring(element.typeName)
        type.dataFetcher(element.fieldName, element.dataFetcher)
        wiringBuilder.type(type)
    }

    override fun visit(element: GraphQLScalarWiringElement) {
        wiringBuilder.scalar(element.scalar)
    }

    override fun visit(element: GraphQLDirectiveWiringElement) {
        wiringBuilder.directive(element.name, element.schemaDirectiveWiring)
    }

    override fun visit(element: GraphQLTypeResolverWiringElement) {
        val typeResolver = TypeRuntimeWiring.newTypeWiring(element.typeName)
        typeResolver.typeResolver(element.typeResolver)
        wiringBuilder.type(typeResolver)
    }
}