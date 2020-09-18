//
// MIT License
//
// Copyright (c) 2020 Alexander Söderberg
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files (the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions:
//
// The above copyright notice and this permission notice shall be included in all
// copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
// SOFTWARE.
//
package com.intellectualsites.commands.bungee;

import com.intellectualsites.commands.Command;
import com.intellectualsites.commands.arguments.CommandArgument;
import com.intellectualsites.commands.internal.CommandRegistrationHandler;
import com.intellectualsites.commands.meta.SimpleCommandMeta;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;

final class BungeePluginRegistrationHandler<C> implements
        CommandRegistrationHandler<SimpleCommandMeta> {

    private final Map<CommandArgument<?, ?>, net.md_5.bungee.api.plugin.Command> registeredCommands = new HashMap<>();

    private BungeeCommandManager<C> bungeeCommandManager;

    BungeePluginRegistrationHandler() {
    }

    void initialize(@Nonnull final BungeeCommandManager<C> bungeeCommandManager) {
        this.bungeeCommandManager = bungeeCommandManager;
    }

    @Override
    public boolean registerCommand(@Nonnull final Command<?, SimpleCommandMeta> command) {
        /* We only care about the root command argument */
        final CommandArgument<?, ?> commandArgument = command.getArguments().get(0);
        if (this.registeredCommands.containsKey(commandArgument)) {
            return false;
        }
        @SuppressWarnings("unchecked") final BungeeCommand<C> bungeeCommand = new BungeeCommand<>(
                (Command<C, SimpleCommandMeta>) command,
                (CommandArgument<C, ?>) commandArgument,
                this.bungeeCommandManager);
        this.registeredCommands.put(commandArgument, bungeeCommand);
        this.bungeeCommandManager.getOwningPlugin().getProxy().getPluginManager()
                .registerCommand(this.bungeeCommandManager.getOwningPlugin(), bungeeCommand);
        return true;
    }

}
