package engine.ext

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import graphql.ExecutionInput
import graphql.execution.preparsed.PreparsedDocumentEntry
import graphql.execution.preparsed.PreparsedDocumentProvider
import graphql.execution.preparsed.persisted.PersistedQueryCache
import graphql.execution.preparsed.persisted.PersistedQueryCacheMiss
import java.util.function.Function


interface PreparsedDocumentProviderCache {
    fun computeIfAbsent(key: Any, computeFunc: Function<Any, PreparsedDocumentEntry>): PreparsedDocumentEntry
}

class InMemoryPreparsedDocumentProviderCache : PreparsedDocumentProviderCache {

    private val cache: Cache<Any, PreparsedDocumentEntry> = Caffeine.newBuilder().maximumSize(10_000).build()

    override fun computeIfAbsent(key: Any, computeFunc: Function<Any, PreparsedDocumentEntry>): PreparsedDocumentEntry {
        return cache.get(key) { computeFunc.apply(it) }!!
    }
}

/**
 * Caches parsed queries to remove the need for re-parsing/validating each time
 *
 * See https://www.graphql-java.com/documentation/v15/execution/
 */
class CachedPreparsedDocumentProvider(
    private val cache: PreparsedDocumentProviderCache = InMemoryPreparsedDocumentProviderCache()
) : PreparsedDocumentProvider {

    override fun getDocument(
        executionInput: ExecutionInput,
        parseAndValidateFunction: Function<ExecutionInput, PreparsedDocumentEntry>
    ): PreparsedDocumentEntry? {
        return cache.computeIfAbsent(executionInput.query) {
            parseAndValidateFunction.apply(executionInput)
        }
    }
}


class InMemoryPersistedQueryCache(
    private val cache: PreparsedDocumentProviderCache = InMemoryPreparsedDocumentProviderCache()
) : PersistedQueryCache {

    override fun getPersistedQueryDocument(
        persistedQueryId: Any,
        executionInput: ExecutionInput,
        onCacheMiss: PersistedQueryCacheMiss
    ): PreparsedDocumentEntry? {
        return cache.computeIfAbsent(persistedQueryId) {
            onCacheMiss.apply(executionInput.query)
        }
    }
}
