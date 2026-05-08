package de.paul.voxelgame.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ChatSystem {
    private static final int MAX_MESSAGES = 8;
    private static final int MAX_HISTORY = 32;
    private static final double MESSAGE_HOLD_SECONDS = 5.0;
    private static final double MESSAGE_FADE_SECONDS = 2.0;

    private final List<ChatMessage> messages = new ArrayList<>();
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

    public void update(final double deltaSeconds) {
        if (open || messages.isEmpty()) {
            return;
        }

        final double delta = Math.max(0.0, deltaSeconds);
        final double maxAge = MESSAGE_HOLD_SECONDS + MESSAGE_FADE_SECONDS;
        for (int i = messages.size() - 1; i >= 0; i--) {
            final ChatMessage message = messages.get(i);
            message.ageSeconds += delta;
            if (message.ageSeconds >= maxAge) {
                messages.remove(i);
            }
        }
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
        messages.add(new ChatMessage(message));
        while (messages.size() > MAX_MESSAGES) {
            messages.remove(0);
        }
    }

    public List<String> recentMessages() {
        final List<String> result = new ArrayList<>();
        for (final ChatMessage message : messages) {
            result.add(message.text);
        }
        return Collections.unmodifiableList(result);
    }

    public List<VisibleMessage> visibleMessages() {
        final List<VisibleMessage> result = new ArrayList<>();
        for (final ChatMessage message : messages) {
            final float alpha = open ? 1.0f : alphaForAge(message.ageSeconds);
            if (alpha > 0.0f) {
                result.add(new VisibleMessage(message.text, alpha));
            }
        }
        return Collections.unmodifiableList(result);
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

    private float alphaForAge(final double ageSeconds) {
        if (ageSeconds <= MESSAGE_HOLD_SECONDS) {
            return 1.0f;
        }
        final double fadeAge = ageSeconds - MESSAGE_HOLD_SECONDS;
        return (float) Math.max(0.0, Math.min(1.0, 1.0 - fadeAge / MESSAGE_FADE_SECONDS));
    }

    public record VisibleMessage(String text, float alpha) {
    }

    private static final class ChatMessage {
        private final String text;
        private double ageSeconds;

        private ChatMessage(final String text) {
            this.text = text;
        }
    }
}
