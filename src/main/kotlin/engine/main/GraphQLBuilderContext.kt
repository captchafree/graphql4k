package engine.main

import org.dataloader.DataLoader
import java.util.function.Supplier

sealed class DataLoaderBlueprint(
    open val key: String
) {

    /**
     * Creates a new data loader for each query
     */
    data class OnDemandDataLoader(
        override val key: String,
        val loader: Supplier<out DataLoader<*, *>>
    ) : DataLoaderBlueprint(key)

    /**
     * Reuses the same data loader for each query
     */
    data class PersistentDataLoader(
        override val key: String,
        val loader: DataLoader<*, *>
    ): DataLoaderBlueprint(key)


    fun get(): DataLoader<*, *> = when (this) {
        is OnDemandDataLoader -> this.loader.get()
        is PersistentDataLoader -> this.loader
    }
}


class GraphQLBlueprintRegistry {

    var instanceFactory: InstanceFactory = SimpleInstanceFactory()

    val elements: MutableList<GraphQLWiringElement> = mutableListOf()

    fun addDefinition(element: GraphQLWiringElement) {
        elements.add(element)
    }

}
