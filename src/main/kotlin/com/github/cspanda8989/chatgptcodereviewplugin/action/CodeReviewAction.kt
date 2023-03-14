package com.github.cspanda8989.chatgptcodereviewplugin.action

import com.fasterxml.jackson.databind.ObjectMapper
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
import com.theokanning.openai.service.OpenAiService.*
import okhttp3.OkHttpClient
import java.net.InetSocketAddress
import java.net.Proxy
import java.time.Duration
import java.util.Arrays.asList


class CodeReviewAction : AnAction() {
    var PRESET_PROMPT_CODE_REVIEW_ZH =
        "你是一个高级程序员，专业严格认真且真诚，请评审以下代码，并列出待优化列表，写明代办。\n任何格式错误、并发隐患、非线程安全、不符合开发习惯、不符合设计模式、拼写错误、bug、复杂度都属于优化问题。\n无需举例。\n代码非常好无优化项时就说:GoodJob!"


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

                    var systemChatMessage = ChatMessage("system", PRESET_PROMPT_CODE_REVIEW_ZH)
                    var inputPromptMessage = ChatMessage("user", "审核：" + selectedText)


                    val proxy = Proxy(Proxy.Type.HTTP, InetSocketAddress("127.0.0.1", 7890))
                    val mapper: ObjectMapper = defaultObjectMapper()
                    val client: OkHttpClient = defaultClient(apiKey, Duration.ofMinutes(1))
                        .newBuilder()
                        .proxy(proxy)
                        .build()
                    val retrofit = defaultRetrofit(client, mapper)

                    val api: OpenAiApi = retrofit.create(OpenAiApi::class.java)
                    val service = OpenAiService(api)

                    var req = ChatCompletionRequest.builder()
                        .model("gpt-3.5-turbo")
                        .messages(asList(systemChatMessage, inputPromptMessage))
                        .build()
                    var ret = service.createChatCompletion(req).choices
                    if (ret.size > 0) {
                        var result = ret[0]
                        var content = "    /*todo:" + result.message.content
                        content = content.replace("\n", "\n      ")

                        val project: Project? = editor.project
                        val document: Document = editor.document

                        WriteCommandAction.runWriteCommandAction(project) {
                            document.insertString(
                                selectionModel.selectionStart, content + " */\n"
                            )
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
