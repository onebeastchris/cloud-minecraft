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
package org.incendo.cloud.examples.bukkit.builder.feature;

import io.leangen.geantyref.TypeToken;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.incendo.cloud.bukkit.BukkitCommandManager;
import org.incendo.cloud.description.Description;
import org.incendo.cloud.examples.bukkit.ExamplePlugin;
import org.incendo.cloud.examples.bukkit.builder.BuilderFeature;
import org.incendo.cloud.key.CloudKey;

import static org.incendo.cloud.bukkit.parser.WorldParser.worldParser;
import static org.incendo.cloud.parser.aggregate.AggregateParserTripletBuilder.directMapper;
import static org.incendo.cloud.parser.standard.IntegerParser.integerParser;

public final class CompoundArgumentExample implements BuilderFeature {

    @Override
    public void registerFeature(
            final @NonNull ExamplePlugin examplePlugin,
            final @NonNull BukkitCommandManager<CommandSender> manager
    ) {
        final CloudKey<World> worldKey = CloudKey.of("world", World.class);
        final CloudKey<Vector> coordsKey = CloudKey.of("coords", Vector.class);

        manager.command(manager.commandBuilder("builder")
                .literal("teleport")
                .literal("me")
                // Require a player sender
                .senderType(Player.class)
                .required(worldKey, worldParser(), Description.of("World name"))
                .requiredArgumentTriplet(
                        coordsKey,
                        TypeToken.get(Vector.class),
                        "x", integerParser(),
                        "y", integerParser(),
                        "z", integerParser(),
                        directMapper((sender, x, y, z) -> new Vector(x, y, z)),
                        Description.of("Coordinates")
                )
                .handler(context -> {
                    final Player player = context.sender();
                    final World world = context.get(worldKey);
                    final Vector coords = context.get(coordsKey);
                    final Location location = coords.toLocation(world);
                    player.teleport(location);
                }));
    }
}
