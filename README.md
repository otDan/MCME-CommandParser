# MCME Command Parser
This command parser is made for Minecraft plugins as well as Bungee proxy plugins. It provides command parsing with automated tab complete suggestion generation.
## Use as Maven Dependency
Clone this repository into your IDE and install by maven command <b>mvn install</b>. Then in your project add to your dependencies in pom.xml: 

        <dependency>
            <groupId>com.mcmiddleearth</groupId>
            <artifactId>MCME-CommandParser</artifactId>
            <version>1.0</version>
        </dependency>

## Getting Started
First you need to implement interface <b>McmeCommandSender</b> to wrap information about your command senders. In a Bukkit plugin it will usually wrap Player objects. In a Bungee plugin it will usually wrap ProxiedPlayer objects.
Second you need to subclass <b>AbstractCommandHandler</b> class. Here you have to implement <b>createCommandTree</b> method to provide the Brigadier command tree you want to use.<br> 
<u>Important</u>: The base command node is passed to the <b>AbstractCommandHandler constructor</b>, it must not be added to the tree in <b>createCommandTree</b>.<br> 
<u>Example</u>: Say you want to make command <b>/foo</b> with subcommand <b>/foo bar</b>. Your <b>FooCommand</b> class would be like this: 

       public FooCommand() {
            super("foo");
       } 

       @Override
       protected HelpfulLiteralBuilder createCommandTree(HelpfulLiteralBuilder commandNodeBuilder) {
            commandNodeBuilder
                .then(HelpfulLiteralBuilder.literal("bar");
       }
   
To parse and execute a command just pass the command line string including preceding "/" to method <b>execute</b> of your subclass of <b>AbstractCommandHandler</b>. To get command complete suggestions create a <b>TabCompleteRequest</b> with the current partial of the command line input and pass it to <b>onTabComplete</b> method. Subsequently, suggestions can be retrieved from the request object. If there are no suggestions the request object is in cancelled state.

## Command Tree Creation
In the command tree you can use all elements of Brigadier. However, MCME Command Parser provides some helpful builders: <b>HelpfulLiteralBuilder</b> and <b>HelpfulRequiredArgumentBuilder</b> These builders automatically generate some command help and suggestions. For more specific help and suggestions you may create your own "helpful" arguments by implementing interface <b>HelpfulArgumentType</b>.
   



