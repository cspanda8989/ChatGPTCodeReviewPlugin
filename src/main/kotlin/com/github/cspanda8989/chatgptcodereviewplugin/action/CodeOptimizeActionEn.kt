package com.github.cspanda8989.chatgptcodereviewplugin.action

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.cspanda8989.chatgptcodereviewplugin.component.ProxyHelper
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.SelectionModel
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.theokanning.openai.OpenAiApi
import com.theokanning.openai.completion.chat.ChatCompletionRequest
import com.theokanning.openai.completion.chat.ChatMessage
import com.theokanning.openai.service.OpenAiService
import okhttp3.OkHttpClient
import java.net.InetSocketAddress
import java.net.Proxy
import java.time.Duration
import java.util.*

class CodeOptimizeActionEn : AnAction() {
    var PRESET_PROMPT_CODE_REVIEW_EN =
        "You are a senior programmer who is serious and truthful in your profession. Please optimize the following code, or state that it does not need to be optimized then say 'no need to optimize'. If you need to rewrite it, please only return the new code. The criteria for rewriting is that the logic of the original code cannot be changed."


    override fun actionPerformed(event: AnActionEvent) {
        val editor: Editor? = event.getData(com.intellij.openapi.actionSystem.CommonDataKeys.EDITOR)
        if (editor != null) {
            val selectionModel: SelectionModel = editor.selectionModel
            val selectedText: String? = selectionModel.selectedText
            if (selectedText == null || selectedText.isEmpty()) {
                return
            }
            if (selectedText.length > 5000) {
                Messages.showMessageDialog("Selected text is too long.", "Error", Messages.getErrorIcon())
            } else {
                try {
                    val apiKey: String? = System.getenv("OPENAI_API_KEY")
                    if (apiKey == null || apiKey.isEmpty()) {
                        Messages.showMessageDialog(
                            "Could not read API key from environment variables.",
                            "Error",
                            Messages.getErrorIcon()
                        )
                        return
                    }

                    var systemChatMessage = ChatMessage("system", PRESET_PROMPT_CODE_REVIEW_EN)
                    var inputPromptMessage = ChatMessage("user", "Optimize: " + selectedText)


                    val proxy = Proxy(Proxy.Type.HTTP, InetSocketAddress("127.0.0.1", 7890))
                    val mapper: ObjectMapper = OpenAiService.defaultObjectMapper()
                    val client: OkHttpClient = ProxyHelper.getOkHttpClient(apiKey)
                    val retrofit = OpenAiService.defaultRetrofit(client, mapper)

                    val api: OpenAiApi = retrofit.create(OpenAiApi::class.java)
                    val service = OpenAiService(api)

                    var req = ChatCompletionRequest.builder()
                        .model("gpt-3.5-turbo")
                        .messages(Arrays.asList(systemChatMessage, inputPromptMessage))
                        .build()
                    var ret = service.createChatCompletion(req).choices
                    if (ret.size > 0) {
                        var result = ret[0]
                        var content = result.message.content
                        if (!content.contains("no need to optimize")) {
                            val project: Project? = editor.project
                            val document: Document = editor.document
                            val selectionModel = editor.selectionModel
                            val start = selectionModel.selectionStart
                            val end = selectionModel.selectionEnd

                            WriteCommandAction.runWriteCommandAction(project) {
                                document.replaceString(start, end, content)
                            }
                        } else {
                            Messages.showMessageDialog("No need to optimize!", "Info", Messages.getInformationIcon())
                        }
                    } else {
                        Messages.showMessageDialog("No response from OpenAI", "Error", Messages.getErrorIcon())
                    }
                } catch (e: Exception) {
                    Messages.showMessageDialog(
                        "An error occurred while connecting to OpenAI",
                        "Error",
                        Messages.getErrorIcon()
                    )
                }
            }
        }
    }
}