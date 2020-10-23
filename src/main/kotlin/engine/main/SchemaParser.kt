package engine.main

import com.google.common.reflect.ClassPath
import graphql.schema.idl.SchemaParser
import graphql.schema.idl.TypeDefinitionRegistry
import java.util.*

/**
 * Parses a directory for graphql schema definition files and creates a valid [TypeDefinitionRegistry]
 */
interface GraphQLSchemaParser {
    fun parse(): TypeDefinitionRegistry
}

class StringSchemaParser(private val schema: String) : GraphQLSchemaParser {
    override fun parse(): TypeDefinitionRegistry {
        return SchemaParser().parse(schema)
    }
}

/**
 * Scans the resources in the classpath for any files ending with the `schemaFileExtension`.
 * Any discovered files are parsed and merged into the resulting registry.
 */
class ResourcesSchemaParser(
    private val schemaFileExtension: String = ".graphqls"
) : GraphQLSchemaParser {

    override fun parse(): TypeDefinitionRegistry {
        val registry = TypeDefinitionRegistry()
        val parser = SchemaParser()

        ClassPath.from(Thread.currentThread().contextClassLoader).resources
            .filter { it.resourceName.endsWith(schemaFileExtension) }
            .map(this::readResourceContents)
            .map(parser::parse)
            .forEach(registry::merge)

        return registry
    }

    private fun readResourceContents(resourceInfo: ClassPath.ResourceInfo): String {
        val s = Scanner(resourceInfo.asByteSource().openBufferedStream()).useDelimiter("\\A")
        return if (s.hasNext()) s.next() else ""
    }
}