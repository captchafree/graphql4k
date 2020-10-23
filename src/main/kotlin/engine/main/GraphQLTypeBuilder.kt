package engine.main

import graphql.schema.DataFetcher

class GraphQLTypeBuilder(
    blueprintRegistry: GraphQLBlueprintRegistry,
    private val typeName: String
) : GraphQLBuilderBase(blueprintRegistry) {

    fun <T> field(fieldName: String, dataFetcher: DataFetcher<T>) {
        blueprintRegistry.addDefinition(GraphQLFieldWiringElement(getCallerCodePosition(), typeName, fieldName, dataFetcher))
    }
}
