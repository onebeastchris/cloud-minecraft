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
package org.incendo.cloud.brigadier.node;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.brigadier.tree.ArgumentCommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.leangen.geantyref.TypeToken;
import java.util.Arrays;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.incendo.cloud.Command;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.SenderMapper;
import org.incendo.cloud.brigadier.CloudBrigadierManager;
import org.incendo.cloud.brigadier.suggestion.CloudDelegatingSuggestionProvider;
import org.incendo.cloud.execution.ExecutionCoordinator;
import org.incendo.cloud.internal.CommandRegistrationHandler;
import org.incendo.cloud.parser.aggregate.AggregateParser;
import org.incendo.cloud.suggestion.Suggestion;
import org.incendo.cloud.type.tuple.Pair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static com.google.common.truth.Truth.assertThat;
import static org.incendo.cloud.parser.standard.BooleanParser.booleanParser;
import static org.incendo.cloud.parser.standard.IntegerParser.integerParser;
import static org.incendo.cloud.parser.standard.StringParser.greedyStringParser;

@SuppressWarnings("unchecked")
@ExtendWith(MockitoExtension.class)
class LiteralBrigadierNodeFactoryTest {

    private CommandDispatcher<Object> dispatcher;
    private TestCommandManager commandManager;
    private LiteralBrigadierNodeFactory<Object, Object> literalBrigadierNodeFactory;

    @BeforeEach
    void setup() {
        this.dispatcher = new CommandDispatcher<>();
        this.commandManager = new TestCommandManager();
        final CloudBrigadierManager<Object, Object> cloudBrigadierManager = new CloudBrigadierManager<>(
                this.commandManager,
                SenderMapper.identity()
        );
        this.literalBrigadierNodeFactory = cloudBrigadierManager.literalBrigadierNodeFactory();
    }

    @Test
    void testSimple() throws Exception {
        // Arrange
        final Command<Object> command = this.commandManager.commandBuilder("command")
                .literal("literal")
                .required("integer", integerParser(0, 10))
                .optional("string", greedyStringParser(),
                        org.incendo.cloud.suggestion.SuggestionProvider.suggesting(Arrays.asList(
                                Suggestion.suggestion("some"),
                                Suggestion.suggestion("suggestions")
                        ))
                ).build();
        this.commandManager.command(command);
        final com.mojang.brigadier.Command<Object> brigadierCommand = ctx -> 0;

        // Act
        final LiteralCommandNode<Object> commandNode = this.literalBrigadierNodeFactory.createNode(
                "command",
                command,
                brigadierCommand
        );
        this.dispatcher.getRoot().addChild(commandNode);

        // Assert
        assertThat(commandNode).isNotNull();
        assertThat(commandNode.getLiteral()).isEqualTo("command");
        assertThat(commandNode.isValidInput("command")).isTrue();
        assertThat(commandNode.getChildren()).hasSize(1);
        assertThat(commandNode.getCommand()).isNull();

        assertThat(commandNode.getChild("literal")).isNotNull();
        assertThat(commandNode.getChild("literal")).isInstanceOf(LiteralCommandNode.class);
        assertThat(commandNode.getChild("literal").getChildren()).hasSize(1);
        assertThat(commandNode.getCommand()).isNull();

        assertThat(commandNode.getChild("literal").getChild("integer")).isNotNull();
        assertThat(commandNode.getChild("literal").getChild("integer")).isInstanceOf(ArgumentCommandNode.class);
        final ArgumentCommandNode<Object, Integer> integerArgument = (ArgumentCommandNode<Object, Integer>)
                commandNode.getChild("literal").getChild("integer");
        assertThat(integerArgument.getName()).isEqualTo("integer");
        assertThat(integerArgument.getType()).isInstanceOf(IntegerArgumentType.class);
        assertThat(integerArgument.getType()).isEqualTo(IntegerArgumentType.integer(0, 10));
        assertThat(integerArgument.getChildren()).hasSize(1);
        assertThat(integerArgument.getCommand()).isEqualTo(brigadierCommand); // Following is optional.

        assertThat(integerArgument.getChild("string")).isNotNull();
        assertThat(integerArgument.getChild("string")).isInstanceOf(ArgumentCommandNode.class);
        final ArgumentCommandNode<Object, String> stringArgument = (ArgumentCommandNode<Object, String>)
                integerArgument.getChild("string");
        assertThat(stringArgument.getName()).isEqualTo("string");
        assertThat(stringArgument.getType()).isInstanceOf(StringArgumentType.class);
        assertThat(((StringArgumentType) stringArgument.getType()).getType())
                .isEqualTo(StringArgumentType.StringType.GREEDY_PHRASE);
        assertThat(stringArgument.getChildren()).isEmpty();
        assertThat(stringArgument.getCommand()).isEqualTo(brigadierCommand);

        assertThat(stringArgument.getCustomSuggestions()).isInstanceOf(CloudDelegatingSuggestionProvider.class);
        final String suggestionString = "command literal 9 ";
        final SuggestionProvider<Object> suggestionProvider = stringArgument.getCustomSuggestions();
        final Suggestions suggestions = suggestionProvider.getSuggestions(
                this.dispatcher.parse(suggestionString, new Object()).getContext().build(suggestionString),
                new SuggestionsBuilder(suggestionString, suggestionString.length())
        ).get();
        assertThat(suggestions.getList().stream().map(com.mojang.brigadier.suggestion.Suggestion::getText))
                .containsExactly("some", "suggestions");
    }

    @Test
    void testAggregate() {
        // Arrange
        final Command<Object> command = this.commandManager.commandBuilder("command")
                .literal("literal")
                .required(
                        "aggregate",
                        AggregateParser.builder()
                                .withComponent("integer", integerParser(0, 10))
                                .withComponent("string", greedyStringParser())
                                .withDirectMapper(
                                        new TypeToken<Pair<Integer, String>>() {},
                                        (cmdCtx, ctx) -> Pair.of(ctx.<Integer>get("integer"), ctx.<String>get("string"))
                                )
                                .build()
                )
                .required("boolean", booleanParser())
                .build();
        this.commandManager.command(command);
        final com.mojang.brigadier.Command<Object> brigadierCommand = ctx -> 0;

        // Act
        final LiteralCommandNode<Object> commandNode = this.literalBrigadierNodeFactory.createNode(
                "command",
                command,
                brigadierCommand
        );

        // Assert
        assertThat(commandNode).isNotNull();
        assertThat(commandNode.getLiteral()).isEqualTo("command");
        assertThat(commandNode.isValidInput("command")).isTrue();
        assertThat(commandNode.getChildren()).hasSize(1);
        assertThat(commandNode.getCommand()).isNull();

        assertThat(commandNode.getChild("literal")).isNotNull();
        assertThat(commandNode.getChild("literal")).isInstanceOf(LiteralCommandNode.class);
        assertThat(commandNode.getChild("literal").getChildren()).hasSize(1);
        assertThat(commandNode.getCommand()).isNull();

        assertThat(commandNode.getChild("literal").getChild("integer")).isNotNull();
        assertThat(commandNode.getChild("literal").getChild("integer")).isInstanceOf(ArgumentCommandNode.class);
        final ArgumentCommandNode<Object, Integer> integerArgument = (ArgumentCommandNode<Object, Integer>)
                commandNode.getChild("literal").getChild("integer");
        assertThat(integerArgument.getName()).isEqualTo("integer");
        assertThat(integerArgument.getType()).isInstanceOf(IntegerArgumentType.class);
        assertThat(integerArgument.getType()).isEqualTo(IntegerArgumentType.integer(0, 10));
        assertThat(integerArgument.getChildren()).hasSize(1);
        assertThat(integerArgument.getCommand()).isNull();

        assertThat(integerArgument.getChild("string")).isNotNull();
        assertThat(integerArgument.getChild("string")).isInstanceOf(ArgumentCommandNode.class);
        final ArgumentCommandNode<Object, String> stringArgument = (ArgumentCommandNode<Object, String>)
                integerArgument.getChild("string");
        assertThat(stringArgument.getName()).isEqualTo("string");
        assertThat(stringArgument.getType()).isInstanceOf(StringArgumentType.class);
        assertThat(((StringArgumentType) stringArgument.getType()).getType())
                .isEqualTo(StringArgumentType.StringType.GREEDY_PHRASE);
        assertThat(stringArgument.getChildren()).hasSize(1);
        assertThat(stringArgument.getCommand()).isNull();

        assertThat(stringArgument.getChild("boolean")).isNotNull();
        assertThat(stringArgument.getChild("boolean")).isInstanceOf(ArgumentCommandNode.class);
        final ArgumentCommandNode<Object, Boolean> booleanArgument = (ArgumentCommandNode<Object, Boolean>)
                stringArgument.getChild("boolean");
        assertThat(booleanArgument.getName()).isEqualTo("boolean");
        assertThat(booleanArgument.getType()).isInstanceOf(BoolArgumentType.class);
        assertThat(booleanArgument.getChildren()).isEmpty();
        assertThat(booleanArgument.getCommand()).isEqualTo(brigadierCommand);
    }


    private static final class TestCommandManager extends CommandManager<Object> {

        private TestCommandManager() {
            super(ExecutionCoordinator.simpleCoordinator(), CommandRegistrationHandler.nullCommandRegistrationHandler());
        }

        @Override
        public boolean hasPermission(final @NonNull Object sender, final @NonNull String permission) {
            return true;
        }
    }
}
