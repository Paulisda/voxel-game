package de.paul.voxelgame.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ChatSystem {
    private static final int MAX_MESSAGES = 8;
    private static final int MAX_HISTORY = 32;

    private final List<String> messages = new ArrayList<>();
    private final List<String> history = new ArrayList<>();
    private List<String> suggestions = Collections.emptyList();
    private boolean open;
    private String input = "";
    private int historyCursor = -1;

    public void openChat() {
        open = true;
        input = "";
        historyCursor = -1;
    }

    public void openCommand() {
        open = true;
        input = "/";
        historyCursor = -1;
    }

    public void close() {
        open = false;
        input = "";
    }

    public boolean isOpen() {
        return open;
    }

    public String input() {
        return input;
    }

    public void setInput(final String input) {
        this.input = input == null ? "" : input;
        historyCursor = -1;
    }

    public void appendText(final String text) {
        if (text == null || text.isEmpty()) {
            return;
        }
        input += text;
    }

    public void removeLastCharacter() {
        if (input.isEmpty()) {
            return;
        }
        input = input.substring(0, input.offsetByCodePoints(input.length(), -1));
    }

    public String submit() {
        final String submitted = input.trim();
        addHistoryEntry(submitted);
        close();
        return submitted;
    }

    public void previousHistory() {
        if (history.isEmpty()) {
            return;
        }
        if (historyCursor < 0) {
            historyCursor = history.size() - 1;
        } else {
            historyCursor = Math.max(0, historyCursor - 1);
        }
        input = history.get(historyCursor);
    }

    public void nextHistory() {
        if (history.isEmpty() || historyCursor < 0) {
            return;
        }
        historyCursor++;
        if (historyCursor >= history.size()) {
            historyCursor = -1;
            input = "";
        } else {
            input = history.get(historyCursor);
        }
    }

    public void setSuggestions(final List<String> suggestions) {
        this.suggestions = suggestions == null ? Collections.emptyList() : List.copyOf(suggestions);
    }

    public List<String> suggestions() {
        return suggestions;
    }

    public void addMessage(final String message) {
        if (message == null || message.isBlank()) {
            return;
        }
        messages.add(message);
        while (messages.size() > MAX_MESSAGES) {
            messages.remove(0);
        }
    }

    public List<String> recentMessages() {
        return Collections.unmodifiableList(messages);
    }

    private void addHistoryEntry(final String submitted) {
        if (submitted == null || submitted.isBlank()) {
            return;
        }
        if (!history.isEmpty() && history.get(history.size() - 1).equals(submitted)) {
            return;
        }
        history.add(submitted);
        while (history.size() > MAX_HISTORY) {
            history.remove(0);
        }
    }
}
