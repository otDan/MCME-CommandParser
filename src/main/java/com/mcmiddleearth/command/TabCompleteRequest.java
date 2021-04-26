package com.mcmiddleearth.command;

import java.util.List;

public interface TabCompleteRequest {

    void setCancelled(boolean cancelled);

    boolean isCancelled();

    McmeCommandSender getSender();

    List<String> getSuggestions();

    String getCursor();
}
