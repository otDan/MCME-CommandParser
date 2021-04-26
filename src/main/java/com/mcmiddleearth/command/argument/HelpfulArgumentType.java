package com.mcmiddleearth.command.argument;

public interface HelpfulArgumentType {

    void setTooltip(String tooltip);

    default String getTooltip() {
        return null;
    }
}
