/*
 * This file is part of HuskHomes, licensed under the Apache License 2.0.
 *
 *  Copyright (c) William278 <will27528@gmail.com>
 *  Copyright (c) contributors
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package net.william278.huskhomes.teleport;

import de.themoep.minedown.adventure.MineDown;
import net.william278.huskhomes.HuskHomes;
import net.william278.huskhomes.config.Settings;
import net.william278.huskhomes.hook.EconomyHook;
import net.william278.huskhomes.position.Position;
import net.william278.huskhomes.user.OnlineUser;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class TimedTeleport extends Teleport {

    public static final String BYPASS_PERMISSION = "huskhomes.bypass_teleport_warmup";
    private final OnlineUser teleporter;
    private final Position startLocation;
    private final double startHealth;
    private int timeLeft;

    protected TimedTeleport(@NotNull OnlineUser executor, @NotNull OnlineUser teleporter, @NotNull Target target,
                            @NotNull Type type, int warmupTime, boolean updateLastPosition,
                            @NotNull List<EconomyHook.Action> actions, @NotNull HuskHomes plugin) {
        super(teleporter, executor, target, type, updateLastPosition, actions, plugin);
        this.startLocation = teleporter.getPosition();
        this.startHealth = teleporter.getHealth();
        this.timeLeft = Math.max(warmupTime, 0);
        this.teleporter = teleporter;
    }

    @Override
    public void execute() throws TeleportationException {
        // Check if the teleporter can bypass warmup
        if (timeLeft == 0 || teleporter.hasPermission(BYPASS_PERMISSION)) {
            super.execute();
            return;
        }

        // Check if the teleporter is already warming up to teleport
        if (plugin.isWarmingUp(teleporter.getUuid())) {
            throw new TeleportationException(TeleportationException.Type.ALREADY_WARMING_UP);
        }

        // Validate economy actions
        validateEconomyActions();

        // Check if they are moving at the start of the teleport
        if (teleporter.isMoving()) {
            throw new TeleportationException(TeleportationException.Type.WARMUP_ALREADY_MOVING);
        }

        // Process the warmup and execute the teleport
        this.process();
    }

    // Execute the warmup, fire the event, then execute the teleport if warmup completes normally
    private void process() {
        plugin.fireEvent(plugin.getTeleportWarmupEvent(this, timeLeft), (event) -> {
            plugin.getCurrentlyOnWarmup().add(teleporter.getUuid());
            plugin.getLocales().getLocale("teleporting_warmup_start", Integer.toString(timeLeft))
                    .ifPresent(teleporter::sendMessage);

            // Run the warmup
            final AtomicInteger warmupTaskId = new AtomicInteger();
            final Runnable countdownRunnable = (() -> {
                // Display countdown action bar message
                if (timeLeft > 0) {
                    plugin.getSettings().getSoundEffect(Settings.SoundEffectAction.TELEPORTATION_WARMUP)
                            .ifPresent(teleporter::playSound);
                    plugin.getLocales().getLocale("teleporting_action_bar_warmup", Integer.toString(timeLeft))
                            .ifPresent(this::sendStatusMessage);
                } else {
                    plugin.getLocales().getLocale("teleporting_action_bar_processing")
                            .ifPresent(this::sendStatusMessage);
                    super.execute();
                }

                // Tick (decrement) the timed teleport timer and end it if done
                if (tickAndGetIfDone()) {
                    plugin.getCurrentlyOnWarmup().remove(teleporter.getUuid());
                    plugin.cancelTask(warmupTaskId.get());
                }
            });
            warmupTaskId.set(plugin.runAsyncRepeating(countdownRunnable, 20L));
        });
    }

    /**
     * Ticks a timed teleport, decrementing the time left until the teleport is complete
     * <p>
     * A timed teleport will be cancelled if certain criteria are met:
     * <ul>
     *     <li>The player has left the server</li>
     *     <li>The plugin is disabling</li>
     *     <li>The player has moved beyond the movement threshold from when the warmup started</li>
     *     <li>The player has taken damage (though they may heal, have status ailments or lose/gain hunger)</li>
     * </ul>
     *
     * @return {@code true} if the warmup is complete, {@code false} otherwise
     */
    private boolean tickAndGetIfDone() {
        if (timeLeft <= 0) {
            return true;
        }

        // Cancel the timed teleport if the player takes damage
        if (hasTeleporterTakenDamage()) {
            plugin.getLocales().getLocale("teleporting_cancelled_damage")
                    .ifPresent(teleporter::sendMessage);
            plugin.getLocales().getLocale("teleporting_action_bar_cancelled")
                    .ifPresent(this::sendStatusMessage);
            plugin.getSettings().getSoundEffect(Settings.SoundEffectAction.TELEPORTATION_CANCELLED)
                    .ifPresent(teleporter::playSound);
            return true;
        }

        // Cancel the timed teleport if the player moves
        if (hasTeleporterMoved()) {
            plugin.getLocales().getLocale("teleporting_cancelled_movement")
                    .ifPresent(teleporter::sendMessage);
            plugin.getLocales().getLocale("teleporting_action_bar_cancelled")
                    .ifPresent(this::sendStatusMessage);
            plugin.getSettings().getSoundEffect(Settings.SoundEffectAction.TELEPORTATION_CANCELLED)
                    .ifPresent(teleporter::playSound);
            return true;
        }

        // Decrement the countdown timer
        timeLeft--;
        return false;
    }

    private void sendStatusMessage(@NotNull MineDown message) {
        teleporter.sendMessage(message, plugin.getSettings().getTeleportWarmupDisplay());
    }

    private boolean hasTeleporterMoved() {
        final double maxMovementDistance = 0.1d;
        double movementDistance = Math.abs(startLocation.getX() - teleporter.getPosition().getX()) +
                                  Math.abs(startLocation.getY() - teleporter.getPosition().getY()) +
                                  Math.abs(startLocation.getZ() - teleporter.getPosition().getZ());
        return movementDistance > maxMovementDistance;
    }

    private boolean hasTeleporterTakenDamage() {
        return teleporter.getHealth() < startHealth;
    }

}
