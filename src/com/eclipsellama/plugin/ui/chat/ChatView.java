package com.eclipsellama.plugin.ui.chat;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.part.ViewPart;

import com.eclipsellama.plugin.core.ChatMessage;
import com.eclipsellama.plugin.core.OllamaClient;
import com.eclipsellama.plugin.preferences.EclipseLlamaPreferences;

/**
 * Modern chat view for EclipseLlama.
 * Features: conversation history, model selector, streaming responses.
 */
public class ChatView extends ViewPart {

    public static final String ID = "com.eclipsellama.plugin.view.chat";

    private StyledText chatArea;
    private Text inputField;
    private Button sendButton;
    private Button stopButton;
    private Combo modelCombo;
    private Label statusLabel;

    private final List<ChatMessage> conversation = new ArrayList<>();
    private boolean isStreaming = false;
    private StringBuilder currentResponse;

    // Colors for chat
    private Color userColor;
    private Color assistantColor;
    private Color codeBackground;
    private Font codeFont;

    @Override
    public void createPartControl(Composite parent) {
        Display display = parent.getDisplay();

        // Initialize colors
        userColor = new Color(display, 0, 100, 200);
        assistantColor = new Color(display, 50, 150, 50);
        codeBackground = new Color(display, 40, 44, 52);

        // Create code font
        FontData[] fontData = parent.getFont().getFontData();
        codeFont = new Font(display, "Consolas", fontData[0].getHeight(), SWT.NORMAL);

        parent.setLayout(new GridLayout(1, false));

        createToolbar(parent);
        createChatArea(parent);
        createInputArea(parent);

        // Add system message
        addSystemMessage();
        refreshModels();
    }

    private void createToolbar(Composite parent) {
        Composite toolbar = new Composite(parent, SWT.NONE);
        toolbar.setLayout(new GridLayout(4, false));
        toolbar.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        Label modelLabel = new Label(toolbar, SWT.NONE);
        modelLabel.setText("Model:");

        modelCombo = new Combo(toolbar, SWT.DROP_DOWN | SWT.READ_ONLY);
        modelCombo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        modelCombo.addListener(SWT.Selection, e -> {
            String selected = modelCombo.getText();
            if (!selected.isEmpty()) {
                EclipseLlamaPreferences.setModel(selected);
                EclipseLlamaPreferences.save();
            }
        });

        Button refreshBtn = new Button(toolbar, SWT.PUSH);
        refreshBtn.setText("↻");
        refreshBtn.setToolTipText("Refresh models");
        refreshBtn.addListener(SWT.Selection, e -> refreshModels());

        Button clearBtn = new Button(toolbar, SWT.PUSH);
        clearBtn.setText("Clear");
        clearBtn.setToolTipText("Clear conversation");
        clearBtn.addListener(SWT.Selection, e -> clearConversation());
    }

    private void createChatArea(Composite parent) {
        chatArea = new StyledText(parent, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.WRAP | SWT.READ_ONLY);
        chatArea.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        chatArea.setMargins(10, 10, 10, 10);
    }

    private void createInputArea(Composite parent) {
        Composite inputArea = new Composite(parent, SWT.NONE);
        inputArea.setLayout(new GridLayout(3, false));
        inputArea.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        inputField = new Text(inputArea, SWT.BORDER | SWT.MULTI | SWT.WRAP | SWT.V_SCROLL);
        GridData inputData = new GridData(SWT.FILL, SWT.CENTER, true, false);
        inputData.heightHint = 60;
        inputField.setLayoutData(inputData);
        inputField.setMessage("Ask EclipseLlama anything... (Enter to send, Shift+Enter for new line)");

        inputField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.keyCode == SWT.CR && (e.stateMask & SWT.SHIFT) == 0) {
                    e.doit = false;
                    sendMessage();
                }
            }
        });

        sendButton = new Button(inputArea, SWT.PUSH);
        sendButton.setText("Send");
        sendButton.setLayoutData(new GridData(SWT.CENTER, SWT.FILL, false, false));
        sendButton.addListener(SWT.Selection, e -> sendMessage());

        stopButton = new Button(inputArea, SWT.PUSH);
        stopButton.setText("Stop");
        stopButton.setEnabled(false);
        stopButton.setLayoutData(new GridData(SWT.CENTER, SWT.FILL, false, false));
        stopButton.addListener(SWT.Selection, e -> stopStreaming());

        // Status bar
        statusLabel = new Label(parent, SWT.NONE);
        statusLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        statusLabel.setText("Ready");
    }

    private void addSystemMessage() {
        String systemPrompt = "You are EclipseLlama, a helpful AI coding assistant. "
                + "You help developers write, understand, and improve their code. "
                + "Be concise and provide code examples when helpful.";
        conversation.add(ChatMessage.system(systemPrompt));
    }

    private void refreshModels() {
        statusLabel.setText("Loading models...");
        modelCombo.removeAll();

        new Thread(() -> {
            String[] models = OllamaClient.getAvailableModels();
            Display.getDefault().asyncExec(() -> {
                if (models.length == 0) {
                    statusLabel.setText("⚠️ No models found. Is Ollama running?");
                    // Add recommended models anyway
                    for (String model : EclipseLlamaPreferences.getRecommendedCodeModels()) {
                        modelCombo.add(model);
                    }
                } else {
                    for (String model : models) {
                        modelCombo.add(model);
                    }
                    statusLabel.setText("Ready - " + models.length + " models available");
                }

                // Select current model
                String currentModel = EclipseLlamaPreferences.getModel();
                int index = modelCombo.indexOf(currentModel);
                if (index >= 0) {
                    modelCombo.select(index);
                } else if (modelCombo.getItemCount() > 0) {
                    modelCombo.select(0);
                }
            });
        }).start();
    }

    private void sendMessage() {
        String input = inputField.getText().trim();
        if (input.isEmpty() || isStreaming) {
            return;
        }

        // Add user message
        ChatMessage userMsg = ChatMessage.user(input);
        conversation.add(userMsg);
        appendMessage("You", input, userColor);

        // Clear input
        inputField.setText("");

        // Start streaming
        startStreaming();

        String model = modelCombo.getText();
        if (model.isEmpty()) {
            model = EclipseLlamaPreferences.getModel();
        }

        currentResponse = new StringBuilder();

        OllamaClient.streamChat(
                conversation,
                model,
                this::onChunk,
                this::onComplete,
                this::onError);
    }

    private void startStreaming() {
        isStreaming = true;
        sendButton.setEnabled(false);
        stopButton.setEnabled(true);
        statusLabel.setText("🦙 Thinking...");
        appendMessage("EclipseLlama", "", assistantColor);
    }

    private void stopStreaming() {
        isStreaming = false;
        sendButton.setEnabled(true);
        stopButton.setEnabled(false);
        statusLabel.setText("Stopped");
    }

    private void onChunk(String chunk) {
        if (!isStreaming)
            return;

        currentResponse.append(chunk);
        chatArea.append(chunk);
        chatArea.setTopIndex(chatArea.getLineCount() - 1);
    }

    private void onComplete(String fullResponse) {
        isStreaming = false;

        // Add to conversation history
        conversation.add(ChatMessage.assistant(currentResponse.toString()));

        Display.getDefault().asyncExec(() -> {
            sendButton.setEnabled(true);
            stopButton.setEnabled(false);
            statusLabel.setText("Ready");
            chatArea.append("\n\n");
        });
    }

    private void onError(String error) {
        isStreaming = false;

        Display.getDefault().asyncExec(() -> {
            chatArea.append("\n❌ Error: " + error + "\n\n");
            sendButton.setEnabled(true);
            stopButton.setEnabled(false);
            statusLabel.setText("Error occurred");
        });
    }

    private void appendMessage(String sender, String content, Color color) {
        int start = chatArea.getCharCount();
        String header = "━━━ " + sender + " ━━━\n";
        chatArea.append(header);

        // Style the header
        StyleRange headerStyle = new StyleRange();
        headerStyle.start = start;
        headerStyle.length = header.length();
        headerStyle.foreground = color;
        headerStyle.fontStyle = SWT.BOLD;
        chatArea.setStyleRange(headerStyle);

        if (!content.isEmpty()) {
            chatArea.append(content);
            chatArea.append("\n\n");
        }

        chatArea.setTopIndex(chatArea.getLineCount() - 1);
    }

    private void clearConversation() {
        conversation.clear();
        chatArea.setText("");
        addSystemMessage();
        statusLabel.setText("Conversation cleared");
    }

    /**
     * Add context (selected code) to the next message.
     */
    public void setContext(String context, String action) {
        String prompt = switch (action) {
            case "explain" -> "Explain this code:\n```\n" + context + "\n```";
            case "fix" -> "Find and fix any issues in this code:\n```\n" + context + "\n```";
            case "test" -> "Generate unit tests for this code:\n```\n" + context + "\n```";
            case "document" -> "Generate Javadoc documentation for this code:\n```\n" + context + "\n```";
            default -> context;
        };
        inputField.setText(prompt);
        inputField.setFocus();
    }

    @Override
    public void setFocus() {
        inputField.setFocus();
    }

    @Override
    public void dispose() {
        if (userColor != null)
            userColor.dispose();
        if (assistantColor != null)
            assistantColor.dispose();
        if (codeBackground != null)
            codeBackground.dispose();
        if (codeFont != null)
            codeFont.dispose();
        super.dispose();
    }
}
