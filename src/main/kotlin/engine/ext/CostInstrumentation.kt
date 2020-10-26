package engine.ext

import graphql.*
import graphql.execution.ExecutionContext
import graphql.execution.instrumentation.DocumentAndVariables
import graphql.execution.instrumentation.InstrumentationContext
import graphql.execution.instrumentation.InstrumentationState
import graphql.execution.instrumentation.SimpleInstrumentation
import graphql.execution.instrumentation.parameters.InstrumentationExecuteOperationParameters
import graphql.execution.instrumentation.parameters.InstrumentationExecutionParameters
import graphql.execution.instrumentation.parameters.InstrumentationFieldFetchParameters
import graphql.language.Document
import graphql.schema.DataFetcher
import java.util.concurrent.CompletableFuture

/**
 * WIP
 */
class CostInstrumentation(
    private val maxCost: Int
) : SimpleInstrumentation() {

    private val NOOP_DATAFETCHER = DataFetcher { null }

    override fun createState(): InstrumentationState {
        return CostInstrumentationState()
    }


    override fun instrumentExecutionResult(
        executionResult: ExecutionResult,
        parameters: InstrumentationExecutionParameters
    ): CompletableFuture<ExecutionResult> {
        val extensions = executionResult.extensions ?: mutableMapOf()

        val state = parameters.getInstrumentationState<CostInstrumentationState>()

        val data: MutableMap<String, Any> = mutableMapOf("creditsUsed" to state.getCost())

        val exResult = if (state.getCost() > maxCost) {
            data["creditLimit"] = maxCost

            ExecutionResultImpl(
                GraphqlErrorBuilder.newError()
                    .errorType(ErrorType.ExecutionAborted)
                    .message("Exceeded credit limit for query. Max allowed credits: $maxCost. Credits required for query: ${state.getCost()}")
                    .build()
            )
        } else {
            ExecutionResultImpl(executionResult.getData(), executionResult.errors, extensions)
        }

        extensions["queryCost"] = data

        return CompletableFuture.completedFuture(exResult)
    }

    override fun instrumentDataFetcher(
        dataFetcher: DataFetcher<*>,
        parameters: InstrumentationFieldFetchParameters
    ): DataFetcher<*> {

        val costDirective = parameters.field.getDirective("cost")
        val cost = costDirective?.let {
            it.arguments[0].value as Int
        } ?: run {
            when {
                parameters.field.name.startsWith("__") -> 0
                dataFetcher is TrivialDataFetcher<*> -> 0
                else -> 0
            }
        }

        val state: CostInstrumentationState = parameters.getInstrumentationState()
        state.addToCost(cost)

        if (state.getCost() > maxCost) {
            // Do nothing
            return NOOP_DATAFETCHER
        }

        return dataFetcher
    }
}

class CostInstrumentationState : InstrumentationState {

    private var cost: Int = 0

    fun addToCost(cost: Int): Int {
        this.cost += cost
        return cost
    }

    fun getCost(): Int {
        return cost
    }
}