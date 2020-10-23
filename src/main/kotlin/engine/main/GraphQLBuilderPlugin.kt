package engine.main

import graphql.schema.GraphQLSchema
import graphql.schema.idl.TypeDefinitionRegistry


interface GraphQLBuilderPlugin {

    fun transformSchema(schema: GraphQLSchema): GraphQLSchema {
        return schema
    }

    fun transformTypeDefinitionRegistry(typeDefinitionRegistry: TypeDefinitionRegistry): TypeDefinitionRegistry {
        return typeDefinitionRegistry
    }

    fun onBuild(elements: GraphQLWiringElementRegistry) {}

}
