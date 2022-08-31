package org.jetbrains.tinygoplugin.preview

import com.intellij.openapi.components.service
import com.intellij.openapi.project.ProjectManager
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.http.EmptyHttpHeaders
import io.netty.handler.codec.http.FullHttpRequest
import io.netty.handler.codec.http.QueryStringDecoder
import org.jetbrains.ide.HttpRequestHandler
import java.net.URL

class TinyGoPreviewHttpHandler : HttpRequestHandler() {
    override fun isSupported(request: FullHttpRequest): Boolean {
        val urlDecoder = QueryStringDecoder(request.uri())
        return urlDecoder.path().startsWith("/tinygo-preview")
    }

    override fun process(
        urlDecoder: QueryStringDecoder,
        request: FullHttpRequest,
        context: ChannelHandlerContext
    ): Boolean {
        val path = urlDecoder.path()
        return when (getResponseType(path)) {
            RespType.MainPage -> responseMainPage(urlDecoder, request, context)
            RespType.WASM -> responseWasm(urlDecoder, request, context)
            RespType.Hypertext -> responseHypertext(urlDecoder, request, context)
        }
    }

    private fun responseMainPage(
        urlDecoder: QueryStringDecoder,
        request: FullHttpRequest,
        context: ChannelHandlerContext
    ): Boolean {
        if ("project" !in urlDecoder.parameters().keys || "target" !in urlDecoder.parameters().keys)
            return false
        val projectPath = urlDecoder.parameters()["project"]!!.first()
        val target = urlDecoder.parameters()["target"]!!.first()
        val indexHtml = getResource("/tinygo-preview/index.html")?.readText()
            ?.replace("{PROJECT_PATH}", projectPath)
            ?.replace("{TARGET_NAME}", target)
            ?.encodeToByteArray()
            ?: return false
        return sendData(indexHtml, "index.html", request, context.channel(), EmptyHttpHeaders.INSTANCE)
    }

    private fun responseWasm(
        urlDecoder: QueryStringDecoder,
        request: FullHttpRequest,
        context: ChannelHandlerContext
    ): Boolean {
        val projectPath = urlDecoder.parameters()["project"]?.firstOrNull() ?: return false
        val project = ProjectManager.getInstance().openProjects.firstOrNull { it.basePath == projectPath }

        @Suppress("ForbiddenComment")
        val wasm = project?.service<TinyGoPreviewWasmService>()?.getWasm() ?: return false
        return sendData(wasm, "module.wasm", request, context.channel(), EmptyHttpHeaders.INSTANCE)
    }

    private fun responseHypertext(
        urlDecoder: QueryStringDecoder,
        request: FullHttpRequest,
        context: ChannelHandlerContext
    ): Boolean {
        val truePath = urlDecoder.path().removePrefix("/tinygo-preview")
        val hypertext = getResource("/tinygo-preview/$truePath")?.readBytes()
            ?: getResource("/tinygo-preview/playground/$truePath")?.readBytes()
            ?: return false
        return sendData(hypertext, truePath, request, context.channel(), EmptyHttpHeaders.INSTANCE)
    }

    companion object {
        enum class RespType {
            MainPage, WASM, Hypertext
        }

        fun getResponseType(path: String): RespType =
            when (path) {
                "/tinygo-preview" -> RespType.MainPage
                "/tinygo-preview/wasm" -> RespType.WASM
                else -> RespType.Hypertext
            }

        fun getResource(resourcePath: String): URL? = this::class.java.getResource(resourcePath)
    }
}
