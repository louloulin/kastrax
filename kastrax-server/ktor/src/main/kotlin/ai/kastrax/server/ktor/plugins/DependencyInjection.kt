package ai.kastrax.server.ktor.plugins

import ai.kastrax.server.common.api.DebugApi
import ai.kastrax.server.common.api.ExecutionApi
import ai.kastrax.server.common.api.WorkflowApi
import ai.kastrax.server.ktor.service.KtorDebugService
import ai.kastrax.server.ktor.service.KtorExecutionService
import ai.kastrax.server.ktor.service.KtorWorkflowService
import io.ktor.server.application.*
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger

fun Application.configureDependencyInjection() {
    install(Koin) {
        slf4jLogger()
        modules(appModule)
    }
}

val appModule = module {
    // Repositories
    single<ai.kastrax.server.ktor.repository.IWorkflowRepository> { ai.kastrax.server.ktor.repository.WorkflowRepository() }
    single { ai.kastrax.server.ktor.repository.ExecutionRepository() }
    single { ai.kastrax.server.ktor.repository.BreakpointRepository() }
    single { ai.kastrax.server.ktor.repository.DebugSessionRepository() }

    // Services
    single<WorkflowApi> { KtorWorkflowService(get()) }
    single<ExecutionApi> { KtorExecutionService(get(), get()) }
    single<DebugApi> { KtorDebugService(get(), get(), get()) }
}
