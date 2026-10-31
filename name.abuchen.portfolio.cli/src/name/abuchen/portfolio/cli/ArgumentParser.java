package name.abuchen.portfolio.cli;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import name.abuchen.portfolio.cli.command.CliException;
import name.abuchen.portfolio.cli.command.CommandSpec;
import name.abuchen.portfolio.cli.command.ErrorCode;

/** Turns raw arguments into a command and its option values. */
final class ArgumentParser
{
    record Parsed(CommandSpec command, Map<String, String> options)
    {
    }

    private ArgumentParser()
    {
    }

    static Parsed parse(List<String> args) throws CliException
    {
        var name = args.get(0);
        var command = Commands.require(name);

        var options = new LinkedHashMap<String, String>();
        for (int ii = 1; ii < args.size(); ii++)
        {
            var arg = args.get(ii);

            if (!arg.startsWith("--"))
                throw CliException.usage(ErrorCode.UNEXPECTED_ARGUMENT, "Unexpected argument",
                                "Unexpected argument '" + arg + "'. Options look like --name value.");

            var equals = arg.indexOf('=');
            var optionName = equals < 0 ? arg.substring(2) : arg.substring(2, equals);

            if (!command.accepts(optionName))
                throw CliException.usage(ErrorCode.UNKNOWN_OPTION, "Unknown option",
                                "Command '" + name + "' has no option '--" + optionName + "'.");

            if (command.option(optionName).isFlag())
            {
                if (equals >= 0)
                    throw CliException.usage(ErrorCode.INVALID_OPTION_VALUE, "Invalid option value",
                                    "Option '--" + optionName + "' is a switch and takes no value.")
                                    .with("option", optionName).with("value", arg.substring(equals + 1));

                options.put(optionName, "true");
                continue;
            }
            String value;
            if (equals >= 0)
            {
                value = arg.substring(equals + 1);
            }
            else if (ii + 1 < args.size() && !args.get(ii + 1).startsWith("--"))
            {
                value = args.get(++ii);
            }
            else
            {
                throw CliException.usage(ErrorCode.OPTION_VALUE_REQUIRED, "Option value required",
                                "Option '--" + optionName + "' needs a value.");
            }

            options.put(optionName, value);
        }

        for (var option : command.options())
        {
            if (option.required() && !options.containsKey(option.name()))
                throw CliException.usage(ErrorCode.OPTION_REQUIRED, "Option required",
                                "Command '" + name + "' needs the option '--" + option.name() + " <"
                                                + option.valueName() + ">'.").with("option", option.name());
        }

        return new Parsed(command, options);
    }
}
