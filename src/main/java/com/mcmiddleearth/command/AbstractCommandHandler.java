package com.mcmiddleearth.command;

import com.google.common.base.Joiner;
import com.mcmiddleearth.command.builder.HelpfulLiteralBuilder;
import com.mcmiddleearth.command.node.HelpfulNode;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.context.ParsedCommandNode;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public abstract class AbstractCommandHandler {

    private final CommandDispatcher<McmeCommandSender> commandDispatcher = new CommandDispatcher<>();

    String command;

    public AbstractCommandHandler(String command)
    {
        this.command = command;
        commandDispatcher.register(createCommandTree( HelpfulLiteralBuilder.literal(command)));
    }

    /**
     * This method is called by the constructor to create a command tree based on the command passed to the constructor
     * Additional command trees may be added directly to the command dispatcher.
     * @param baseCommandNodeBuilder the base command node builder
     * @return same builder as passed in parameter
     */
    protected abstract HelpfulLiteralBuilder createCommandTree(HelpfulLiteralBuilder baseCommandNodeBuilder);

    public CommandDispatcher<McmeCommandSender> getCommandDispatcher() { return commandDispatcher; }

    public void execute(McmeCommandSender sender, String[] args) {
        execute(sender, command, args);
    }

    public void execute(McmeCommandSender sender, String command, String[] args) {
        try {
            String message = String.format("%s %s", command, Joiner.on(' ').join(args)).trim();
            ParseResults<McmeCommandSender> result = commandDispatcher.parse(message, sender);
            result.getExceptions().entrySet().stream()
                    .findFirst().ifPresent(error -> sender.sendMessage(error.getValue().getMessage()));
            if(result.getExceptions().isEmpty()) {
                if(result.getContext().getNodes().size() > 0
                        && (result.getContext().getCommand()==null
                            || result.getContext().getRange().getEnd() < result.getReader().getString().length())) {
                    //check for possible child nodes to collect suggestions and bake better error message
                    StringBuilder helpMessage;
                    boolean help = false;
                    String parsedCommand = "/" + result.getReader().getString()
                            .substring(0, result.getContext().getRange().getEnd());
                    if(result.getReader().getRemaining().trim().equals("help")){
                        helpMessage = new StringBuilder("Help for command " + parsedCommand + ":");
                        help = true;
                    } else {
                        helpMessage = new StringBuilder("Invalid command syntax.");
                    }
                    CommandNode<McmeCommandSender> parsedNode = result.getContext().getNodes().get(result.getContext().getNodes().size() - 1).getNode();
                    Collection<CommandNode<McmeCommandSender>> children = (result.getContext().getNodes().isEmpty()?new ArrayList<>():parsedNode.getChildren()
                            .stream().filter(node -> node.canUse(result.getContext().getSource())).collect(Collectors.toList()));
                    Map<CommandNode<McmeCommandSender>,String> use = commandDispatcher.getSmartUsage(parsedNode,result.getContext().getSource());
                    if (children.isEmpty()) {
                        if (result.getContext().getCommand() == null) {
                            helpMessage.append(" Maybe you don't have permission.");
                        } else if(!help) {
                            helpMessage.append(" Maybe you want to do:\n").append(parsedCommand);
                        }
                    } else {
                        if(!help) {
                            helpMessage.append(" Maybe you want to do:");
                        }
                        for(Map.Entry<CommandNode<McmeCommandSender>,String> entry: use.entrySet()) {
                            String usageMessage = "";
                            helpMessage.append("\n");
                            String[] visitedNodes = parsedCommand.split(" ");
                            Iterator<ParsedCommandNode<McmeCommandSender>> iterator = result.getContext().getNodes().listIterator();
                            for (String visitedNode : visitedNodes) {
                                helpMessage.append(" ").append(visitedNode);
                                ParsedCommandNode<McmeCommandSender> node = iterator.next();
//                                helpMessage.color((node.getNode() instanceof LiteralCommandNode?Style.LITERAL:Style.ARGUMENT));
                                if ((node.getNode() instanceof HelpfulNode)) {
//                                    helpMessage.toString().event(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
//                                            new Text(new ComponentBuilder(((HelpfulNode) node.getNode()).getTooltip())
//                                                    .color(Style.TOOLTIP).create())));
                                    if(!((HelpfulNode) node.getNode()).getHelpText().equals("")) {
                                        usageMessage = ((HelpfulNode) node.getNode()).getHelpText();
                                    }
                                } else {
//                                    helpMessage.toString().event((HoverEvent)null);
                                }
                            }
                            String[] possibleNodes = entry.getValue().replace('|', ' ').split(" ");
                            CommandNode<McmeCommandSender> node = parsedNode;
                            CommandNode<McmeCommandSender> lastNode = parsedNode;
                            for(String possibleNode : possibleNodes) {
                                helpMessage.append(" ").append(possibleNode);
                                CommandNode<McmeCommandSender> temp = node;
                                node = findDirectChild(node, possibleNode.replaceAll("[()\\[\\]<>]",""));
                                if(node == null) {
                                    node = findDirectChild(lastNode, possibleNode.replaceAll("[()\\[\\]<>]",""));
                                } else {
                                    lastNode = temp;
                                }
//                                helpMessage.toString().color((node instanceof LiteralCommandNode?Style.LITERAL:Style.ARGUMENT));
                                if ((node instanceof HelpfulNode)) {
//                                    helpMessage.toString().event(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
//                                            new Text(new ComponentBuilder(((HelpfulNode) node).getTooltip())
//                                                    .color(Style.TOOLTIP).create())));
                                    if(!((HelpfulNode) node).getHelpText().equals("")) {
                                        usageMessage = ((HelpfulNode) node).getHelpText();
                                    }
                                } else {
//                                    helpMessage.toString().event((HoverEvent) null);
                                }
                            }
                            if(!usageMessage.equals("")) {
                                helpMessage.append(" : ").append(usageMessage);
//                                        .color(Style.HELP)
//                                           .event(new HoverEvent(HoverEvent.Action.SHOW_TEXT,new Text(new ComponentBuilder().create())));
                            }
                        }
                    }
                    sender.sendMessage(helpMessage.toString());
                } else if(result.getContext().getCommand() == null) {
                    sender.sendMessage("Invalid command. Maybe you don't have permission.");
                } else {
                    commandDispatcher.execute(result);
                }
            }
        } catch (CommandSyntaxException e) {
            sender.sendMessage("Internal command parser exception!");
        }
    }

    public void onTabComplete(TabCompleteRequest request) {
        try {
            ParseResults<McmeCommandSender> result = commandDispatcher.parse(request.getCursor().substring(1), request.getSender());
            if(result.getContext().getNodes().isEmpty()) {
                return;
            }
            List<Suggestion> completionSuggestions
                    = commandDispatcher.getCompletionSuggestions(result).get().getList();
            if(completionSuggestions.isEmpty()) {
                request.setCancelled(true);
            } else {
                request.getSuggestions().addAll(completionSuggestions.stream().map(Suggestion::getText).collect(Collectors.toList()));
            }
        } catch (InterruptedException | ExecutionException e) {
            request.getSender().sendMessage("Command tab complete error." + e);
        }
    }

    private CommandNode<McmeCommandSender> findDirectChild(CommandNode<McmeCommandSender> root, String name) {
        for(CommandNode<McmeCommandSender> node: root.getChildren()) {
            if(node.getName().equals(name)) {//found != null) {
                return node;//found;
            }
        }
        return null;
    }

    private void printTree(CommandNode<McmeCommandSender> node) {
        Logger log = Logger.getLogger(this.getClass().getSimpleName());
        log.info(printNode(node, "", "  "));
    }

    private String printNode(CommandNode<McmeCommandSender> node, String message, String indentation) {
        message = message + indentation+node.getClass().getSimpleName()+" "+node.getName()
                +"\n"+indentation+"-use: "+node.getUsageText();
        if(node instanceof HelpfulNode) {
            message = message
                    +"\n"+indentation+"-help: "+((HelpfulNode)node).getHelpText()
                    +"\n"+indentation+"-tool: "+((HelpfulNode)node).getTooltip();
        }
        for(CommandNode<McmeCommandSender> child: node.getChildren()) {
            message = printNode(child,message+"\n",indentation+"    ");
        }
        return message;
    }
}
