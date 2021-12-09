/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.activemq.artemis.utils.actors;

import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.function.ToIntFunction;

public class BoundActor<T> extends ProcessorBase<Object> {

   private static final AtomicIntegerFieldUpdater<BoundActor> SIZE_UPDATER = AtomicIntegerFieldUpdater.newUpdater(BoundActor.class, "size");
   private volatile int size = 0;

   private static final AtomicIntegerFieldUpdater<BoundActor> SCHEDULED_FUSH_UPDATER = AtomicIntegerFieldUpdater.newUpdater(BoundActor.class, "scheduledFlush");
   private volatile int scheduledFlush = 0;

   private static final Object FLUSH = new Object();


   final int maxSize;
   final ToIntFunction<T> sizeGetter;
   ActorListener<T> listener;

   Runnable overLimit;
   Runnable clearLimit;

   public BoundActor(Executor parent, ActorListener<T> listener, int maxSize, ToIntFunction<T> sizeGetter, Runnable overLimit, Runnable clearLimit) {
      super(parent);
      this.listener = listener;
      this.maxSize = maxSize;
      this.sizeGetter = sizeGetter;
      this.overLimit = overLimit;
      this.clearLimit = clearLimit;
   }


   @Override
   protected final void doTask(Object task) {
      if (task == FLUSH) {
         this.scheduledFlush = 0;
         clearLimit.run();
         return;
      }
      try {
         listener.onMessage((T)task);
      } finally {
         SIZE_UPDATER.getAndAdd(this, -sizeGetter.applyAsInt((T)task));
      }
   }

   public void act(T message) {
      int size = SIZE_UPDATER.addAndGet(this, (sizeGetter.applyAsInt((T)message)));
      if (size > maxSize) {
         flush();
      }
      task(message);
   }

   public void flush() {
      if (SCHEDULED_FUSH_UPDATER.compareAndSet(this, 0, 1)) {
         overLimit.run();
         task(FLUSH);
      }
   }



}
