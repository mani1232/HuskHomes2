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

package net.william278.huskhomes.util;

import net.william278.huskhomes.HuskHomes;
import net.william278.huskhomes.position.Location;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public interface TaskRunner {
    int runAsync(@NotNull Runnable runnable, Location location);
    <T> CompletableFuture<T> supplyAsync(@NotNull Supplier<T> supplier, Location location);
    void runSync(@NotNull Runnable runnable, Location location);
    int runAsyncRepeating(@NotNull Runnable runnable, long delay, Location location);
    void runLater(@NotNull Runnable runnable, long delay, Location location);
    void cancelTask(int taskId);
    void cancelAllTasks();
    @NotNull
    HuskHomes getPlugin();
}
