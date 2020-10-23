# GraphQL4k

GraphQL4k makes it easy to define executable graphql schemas in Kotlin. This library is powered by the [GraphQL Java](https://github.com/graphql-java/graphql-java) project

Features:

- Simple, declarative api using Kotlin's [Type-Safe Builders](https://kotlinlang.org/docs/reference/type-safe-builders.html)
- Plays nice with DI frameworks (Guice, Spring, etc.)
- Interactive terminal output. Easily find where each field's data fetcher is defined in the source code!

Contents:

- [Quick Start](#Quick-Start)
- [Basic Usage](#Basic-Usage-Examples)
- [Using Dependency Injection](#Using-Dependency-Injection)
- [Composing Modules](#Composing-Modules)

## Quick Start

```kotlin
    // Create a module
    val module = graphQLModule {
        // Define data fetchers for the query type
        query {
            field("ping") { "pong!" }
        } 
    }

    // Parse the module and build a graphQL kit. By default the schema is built
    // by searching for .graphqls files in the classpath's resources directory
    val (builder, _) = buildGraphQLKit(module)

    // Build the graphql engine and optionally specify additional behavior
    val gql = builder.build()

    // Execute a query
    gql.execute("query { ping }").toSpecification().also { response ->
        println(response) // prints "{data={ping=pong!}}"
    }
```

## Basic Usage Examples

```kotlin
    // Define your datafetchers
    val module = graphQLModule {
        // Define data fetchers for the query type
        query {
            field("ping") { "pong!" }
    
            // Optionally make return type explicit
            field<User>("userById") { env ->
                /* Logic to fetch user */
            }
        }
        
        // Define a type
        type("User") {
            field("username") { env ->
                /* Logic to resolve the field */
            }
        }
        
        // This is the same as calling `type("Mutation") { ... }`        
        mutation {
            field("updateUser") { env ->
                /* Logic to update the user */
            }
        }
        
        // Define a data loader
        batchLoader("users", true /* create new instance for each query */) {
            BatchLoader<String, User> { keys ->
                supplyAsync {
                    /* Logic to fetch users */
                }
            }
        }
        
        scalar(/* scalar implementation */)
        directive("customDirectiveName", /* directive implementation */)
    }


    val options = GraphQLBuildingOptions.newOptions()
            .schemaParser(ResourcesSchemaParser()) // Creates the schema from .graphqls files in resources directory
            .withPlugin(PrettyPrintSchemaPlugin()) // Print the schema in the terminal upon creation
            .build()

    // Parse the modules and build a graphQL kit
    val (builder, executionInputFactory) = buildGraphQLKit(options, module)

    // Build the graphql engine and optionally specify additional behavior
    val gql = builder.build()

    // `ExecutionInputFactory` handles creating the data loader registry (if necessary) for each request
    val executionInput = executionInputFactory
                            .newExecutionInput()
                            .query()

    // Execute a query
    gql.execute(executionInput).toSpecification().also { response ->
        println(response) // prints "{data={ping=pong!}}"
    }

    // Terminal Output:
    /*
    type Query {
    	ping
    	 ∟MainKt$main$module$1.invoke(Main.kt:24)
    	userById
    	 ∟MainKt$main$module$1.invoke(Main.kt:27)
    }
    
    type User {
        username
        ∟MainKt$main$module$1.invoke(Main.kt:34)
    }

    type Mutation {
        updateUser
        ∟MainKt$main$module$1.invoke(Main.kt:41)
    }
    */
```

## Using Dependency Injection

You can also provide an `InstanceFactory` to manage your dependencies.

```kotlin
    /* Example using Guice */

    val injector = Guice.createInjector(/* specify modules */)

    // Delegate instance creation to Guice
    val instanceFactory = object : InstanceFactory {
        override fun <T : Any> getInstance(clazz: KClass<T>): T {
            return injector.getInstance(clazz.java)
        }
    }

    val module = graphQLModule {

        // Get an instance
        val obj = getInstance<Example>()
        
        // or get an instance lazily
        val otherObj by lazyInject<Example>()

        query {
            field("ping") { obj.getValue() }
        }
    }


    val options = GraphQLBuildingOptions.newOptions()
            .instanceFactory(instanceFactory)
            .build()

    // Parse the modules and build a graphQL kit
    val (builder, _) = buildGraphQLKit(options, module)

    // Use the builder to create a GraphQL instance and execute queries
```

## Composing Modules

The building block of GraphQL4k is a *module*. Modules can be combined to create complex graphql schemas.

```kotlin
    // Define a module using the `graphQLModule` function
    val queryModule = graphQLModule {
    
        // Get an instance
        val obj = getInstance<Example>()
        
        // or get an instance lazily
        val otherObj by lazyInject<Example>()

        query {
            field("ping") { obj.getValue() }
        }
    }
    
    // Or use the `GraphQLModule` base class
    class MutationModule : GraphQLModule() {
        override fun createModule() = mutationModule {
            field("updateUser") { env ->
                // Do whatever
            }
        }
    }

    
    // Install the modules
    val masterModule = graphQLModule {
        install(queryModule)
    
        // Use the provided `InstanceFactory` to create the module instance and install it
        install<MutationModule>()
    }
    
    // Use the module as usual
    val (builder, _) = buildGraphQLKit(masterModule)    
```