package engine.ext

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import graphql.ExecutionInput
import graphql.execution.preparsed.PreparsedDocumentEntry
import graphql.execution.preparsed.PreparsedDocumentProvider
import java.util.function.Function


/**
 * Caches parsed queries to remove the need for re-parsing/validating each time
 *
 * See https://www.graphql-java.com/documentation/v15/execution/
 */
class CachedPreparsedDocumentProvider : PreparsedDocumentProvider {

    private val cache: Cache<String, PreparsedDocumentEntry> = Caffeine.newBuilder().maximumSize(10_000).build()

    override fun getDocument(
        executionInput: ExecutionInput,
        computeFunction: Function<ExecutionInput, PreparsedDocumentEntry>
    ): PreparsedDocumentEntry? {
        return cache.get(executionInput.query) {
            computeFunction.apply(executionInput)
        }
    }
}