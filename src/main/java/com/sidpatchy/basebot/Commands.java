package com.sidpatchy.basebot;

import com.sidpatchy.Robin.Discord.Command;

import java.util.List;
import java.util.Objects;

public class Commands {
    private Command help;

    /**
     * Retrieves a list of all available commands.
     *
     * @return a list containing all Command objects.
     */
    public List<Command> getAllCommands() {
        return List.of(
                help
        );
    }

    public Command getHelp() {
        validateCommand(help);
        return help;
    }

    protected void validateCommand(Command command) {
        Objects.requireNonNull(command, "Command cannot be null");
        Objects.requireNonNull(command.getName(), "Command name cannot be null");
        Objects.requireNonNull(command.getUsage(), "Command usage cannot be null");
        Objects.requireNonNull(command.getHelp(), "Command help cannot be null");

        if (command.getName().isEmpty() || command.getUsage().isEmpty() || command.getHelp().isEmpty()) {
            throw new IllegalArgumentException("Command name or usage cannot be empty");
        }

        // If command overview is null, set it to command help
        if (command.getOverview() == null || command.getOverview().isEmpty()) {
            command.setOverview(command.getHelp());
        }
    }
}
