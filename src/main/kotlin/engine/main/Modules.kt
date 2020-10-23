package engine.main


interface GraphQL4KModule {
    fun install(context: GraphQLBlueprintRegistry)
}


abstract class GraphQLModule : GraphQL4KModule {
    override fun install(context: GraphQLBlueprintRegistry) {
        createModule().install(context)
    }

    abstract fun createModule(): AbstractGraphQLModule<*>
}


abstract class AbstractGraphQLModule<T : GraphQLBuilderBase>(
    private val init: T.() -> Unit
) : GraphQL4KModule {

    override fun install(context: GraphQLBlueprintRegistry) {
        buildTarget(context).apply(init)
    }

    protected abstract fun buildTarget(context: GraphQLBlueprintRegistry): T
}

class GraphQLBuilderModule(
    init: GraphQLWiringBuilder.() -> Unit
) : AbstractGraphQLModule<GraphQLWiringBuilder>(init) {

    override fun buildTarget(context: GraphQLBlueprintRegistry): GraphQLWiringBuilder {
        return GraphQLWiringBuilder(context)
    }
}

class GraphQLTypeBuilderModule(
    private val typeName: String,
    init: GraphQLTypeBuilder.() -> Unit
) : AbstractGraphQLModule<GraphQLTypeBuilder>(init) {

    override fun buildTarget(context: GraphQLBlueprintRegistry): GraphQLTypeBuilder {
        return GraphQLTypeBuilder(context, typeName)
    }
}
