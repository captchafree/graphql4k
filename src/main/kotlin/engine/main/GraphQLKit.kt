package engine.main

import graphql.ExecutionInput
import graphql.GraphQL

interface ExecutionInputFactory {
    fun newExecutionInput(): ExecutionInput.Builder
}

data class GraphQLKit(
    val graphQLBuilder: GraphQL.Builder,
    val executionInputFactory: ExecutionInputFactory
) {

    companion object {

        fun buildGraphQLKit(
            options: GraphQLBuildingOptions = GraphQLBuildingOptions(),
            vararg modules: GraphQL4KModule
        ): GraphQLKit {

            val masterModule = if (modules.size == 1) {
                modules.first()
            } else {
                GraphQLBuilderModule() {
                    modules.forEach(::install)
                }
            }

            return masterModule.toGraphQLKit(options)
        }

        fun buildGraphQLKit(
            vararg modules: GraphQL4KModule
        ): GraphQLKit {
            return buildGraphQLKit(GraphQLBuildingOptions(), *modules)
        }


        /*
        /**
         * Creates a new graphql module
         */
        fun graphQLModule(init: GraphQLWiringBuilder.() -> Unit): GraphQLBuilderModule {
            return GraphQLBuilderModule(init)
        }


        /**
         * Creates a new type builder with a specified name
         */
        fun typeModule(typeName: String, init: GraphQLTypeBuilder.() -> Unit): GraphQLTypeBuilderModule {
            return GraphQLTypeBuilderModule(typeName, init)
        }


        fun queryModule(init: GraphQLTypeBuilder.() -> Unit): GraphQLTypeBuilderModule {
            return GraphQLTypeBuilderModule("Query", init)
        }

        fun mutationModule(init: GraphQLTypeBuilder.() -> Unit): GraphQLTypeBuilderModule {
            return GraphQLTypeBuilderModule("Mutation", init)
        }

        fun subscriptionModule(init: GraphQLTypeBuilder.() -> Unit): GraphQLTypeBuilderModule {
            return GraphQLTypeBuilderModule("Subscription", init)
        }
        */
    }
}
