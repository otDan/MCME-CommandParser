package com.mcmiddleearth.command;

import java.util.ArrayList;
import java.util.List;

public class SimpleTabCompleteRequest implements TabCompleteRequest {

    private boolean isCancelled;

    private final McmeCommandSender sender;

    private final List<String> suggestions = new ArrayList<>();

    private final String cursor;

    public SimpleTabCompleteRequest(McmeCommandSender sender, String cursor) {
        this.sender = sender;
        this.cursor = cursor;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.isCancelled = cancelled;
    }

    @Override
    public boolean isCancelled() {
        return isCancelled;
    }

    @Override
    public McmeCommandSender getSender() {
        return sender;
    }

    @Override
    public List<String> getSuggestions() {
        return (isCancelled?null:suggestions);
    }

    @Override
    public String getCursor() {
        return cursor;
    }
}
