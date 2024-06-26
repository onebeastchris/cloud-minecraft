//
// MIT License
//
// Copyright (c) 2024 Incendo
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
package org.incendo.cloud.brigadier.suggestion;

import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import org.apiguardian.api.API;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.brigadier.CloudBrigadierManager;
import org.incendo.cloud.brigadier.parser.WrappedBrigadierParser;
import org.incendo.cloud.component.CommandComponent;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.suggestion.SuggestionFactory;

import static org.incendo.cloud.brigadier.CloudBrigadierCommand.parsedNodes;

/**
 * Produces Brigadier suggestions by invoking the Cloud suggestion provider.
 *
 * @param <C> command sender type
 * @param <S> Brigadier sender type
 * @since 2.0.0
 */
@API(status = API.Status.INTERNAL, since = "2.0.0")
public final class BrigadierSuggestionFactory<C, S> {

    private final CloudBrigadierManager<C, S> cloudBrigadierManager;
    private final CommandManager<C> commandManager;
    private final SuggestionFactory<C, ? extends TooltipSuggestion> suggestionFactory;

    /**
     * Creates a new suggestion factory.
     *
     * @param cloudBrigadierManager the brigadier manager
     * @param commandManager        the command manager
     * @param suggestionFactory     the suggestion factory-producing tooltip suggestions
     */
    public BrigadierSuggestionFactory(
            final @NonNull CloudBrigadierManager<C, S> cloudBrigadierManager,
            final @NonNull CommandManager<C> commandManager,
            final @NonNull SuggestionFactory<C, ? extends TooltipSuggestion> suggestionFactory
    ) {
        this.cloudBrigadierManager = cloudBrigadierManager;
        this.commandManager = commandManager;
        this.suggestionFactory = suggestionFactory;
    }

    /**
     * Builds suggestions for the given component.
     *
     * @param senderContext the brigadier context
     * @param parentNode    the parent command node
     * @param builder       the suggestion builder to generate suggestions with
     * @return future that completes with the suggestions
     */
    public @NonNull CompletableFuture<@NonNull Suggestions> buildSuggestions(
            final com.mojang.brigadier.context.@NonNull CommandContext<S> senderContext,
            final org.incendo.cloud.internal.@Nullable CommandNode<C> parentNode,
            final @NonNull SuggestionsBuilder builder
    ) {
        final C cloudSender = this.cloudBrigadierManager.senderMapper().map(senderContext.getSource());
        final CommandContext<C> commandContext = new CommandContext<>(
            true,
            cloudSender,
            this.commandManager
        );
        commandContext.store(WrappedBrigadierParser.COMMAND_CONTEXT_BRIGADIER_NATIVE_SENDER, senderContext.getSource());
        String command = builder.getInput()
            .substring(parsedNodes(senderContext.getLastChild()).get(0).second().getStart());

        /* Remove namespace */
        final String leading = command.split(" ")[0];
        if (leading.contains(":")) {
            command = command.substring(leading.split(":")[0].length() + 1);
        }

        return this.suggestionFactory.suggest(commandContext.sender(), command).thenApply(suggestionsResult -> {
            /* Filter suggestions that are literal arguments to avoid duplicates, except for root arguments */
            final List<TooltipSuggestion> suggestions = new ArrayList<>(suggestionsResult.list());
            if (parentNode != null) {
                final Set<String> siblingLiterals = parentNode.children().stream()
                        .map(org.incendo.cloud.internal.CommandNode::component)
                        .filter(Objects::nonNull)
                        .filter(c -> c.type() == CommandComponent.ComponentType.LITERAL)
                        .flatMap(commandComponent -> commandComponent.aliases().stream())
                        .collect(Collectors.toSet());

                suggestions.removeIf(suggestion -> siblingLiterals.contains(suggestion.suggestion()));
            }

            final int trimmed = builder.getInput().length() - suggestionsResult.commandInput().length();
            final int rawOffset = suggestionsResult.commandInput().cursor();
            final SuggestionsBuilder suggestionsBuilder = builder.createOffset(rawOffset + trimmed);

            for (final TooltipSuggestion suggestion : suggestions) {
                try {
                    suggestionsBuilder.suggest(Integer.parseInt(suggestion.suggestion()), suggestion.tooltip());
                } catch (final NumberFormatException e) {
                    suggestionsBuilder.suggest(suggestion.suggestion(), suggestion.tooltip());
                }
            }

            return suggestionsBuilder.build();
        });
    }
}
