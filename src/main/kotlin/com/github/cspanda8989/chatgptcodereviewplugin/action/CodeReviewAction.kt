package com.github.cspanda8989.chatgptcodereviewplugin.action

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.SelectionModel
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.unfbx.chatgpt.OpenAiClient
import com.unfbx.chatgpt.entity.chat.Message
import org.jdom.filter2.Filters.document
import java.net.InetSocketAddress
import java.net.Proxy
import java.util.Arrays.asList


class CodeReviewAction : AnAction() {
    var PRESET_PROMPT_CODE_REVIEW_ZH =
        "你的职责是架构师，评审以下代码，并列出待优化列表，格式为方法名：代办项。如果代码没有问题，就说无需优化。线程不安全、不符合设计模式、拼写错误、bug、复杂度都属于优化问题"


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

                    //val service = OpenAiService(apiKey)

                    var systemChatMessage = Message("system", PRESET_PROMPT_CODE_REVIEW_ZH)
                    var inputPromptMessage = Message("user", selectedText)

                    val proxy = Proxy(Proxy.Type.HTTP, InetSocketAddress("127.0.0.1", 7890))

                    val service = OpenAiClient.builder()
                        .apiKey(apiKey)
                        .connectTimeout(50)
                        .writeTimeout(50)
                        .readTimeout(50)
                        .proxy(proxy)
                        .apiHost("https://api.openai.com/")
                        .build();


                    var ret = service.chatCompletion(asList(systemChatMessage, inputPromptMessage)).choices
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
