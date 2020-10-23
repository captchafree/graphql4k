package engine.ext

import engine.main.GraphQLBuilderPlugin
import engine.main.GraphQLWiringElementRegistry
import engine.main.SchemaPrintingVisitor

/**
 * Prints out the schema upon creation.
 * The output also contains the location in the source code for each datafetcher.
 */
class PrettyPrintSchemaPlugin : GraphQLBuilderPlugin {
    override fun onBuild(elements: GraphQLWiringElementRegistry) {
        val visitor = SchemaPrintingVisitor()
        visitor.visit(elements)
    }
}
