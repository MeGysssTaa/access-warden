/*
 * Copyright 2021 German Vekhorev (DarksideCode)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package me.darksidecode.accesswarden.api;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

public final class FilteredContext {

    private static final int MAX_CALL_STACK_SIZE = 5000;

    private final List<StackTraceElement> callStack;

    FilteredContext(List<StackTraceElement> callStack) throws UnexpectedSetupException {
        if (callStack == null)
            throw new UnexpectedSetupException(
                    "call stack cannot be null");

        if (callStack.isEmpty())
            throw new UnexpectedSetupException(
                    "call stack cannot be empty");

        if (callStack.size() > MAX_CALL_STACK_SIZE)
            throw new UnexpectedSetupException(
                    "call stack cannot contain more than " + MAX_CALL_STACK_SIZE + " elements");

        this.callStack = callStack;
    }

    public List<StackTraceElement> filteredCallStack() {
        return callStack;
    }

    public StackTraceElement mostRecentCall() {
        return callStack.get(0);
    }
    
    public StackTraceElement mostRecentCall(Predicate<StackTraceElement> criteria) {
        if (criteria == null)
            throw new NullPointerException("criteria cannot be null");

        for (StackTraceElement frame : callStack)
            if (criteria.test(frame))
                return frame;

        return null; // no frames matching the given criteria
    }

    public StackTraceElement mostRecentReflectionCall() {
        return mostRecentCall(ContextResolution::isReflectionFrame);
    }

    public StackTraceElement mostRecentNonReflectionCall() {
        return mostRecentCall(frame -> !ContextResolution.isReflectionFrame(frame));
    }

    public StackTraceElement mostRecentNativeCall() {
        return mostRecentCall(StackTraceElement::isNativeMethod);
    }

    public StackTraceElement mostRecentNonNativeCall() {
        return mostRecentCall(frame -> !frame.isNativeMethod());
    }

    public StackTraceElement mostRecentNonReflectionNonNativeCall() {
        return mostRecentCall(frame -> !ContextResolution.isReflectionFrame(frame) && !frame.isNativeMethod());
    }

    public StackTraceElement leastRecentCall() {
        return callStack.get(callStack.size() - 1);
    }

    public StackTraceElement leastRecentCall(Predicate<StackTraceElement> criteria) {
        if (criteria == null)
            throw new NullPointerException("criteria cannot be null");
        
        for (int i = callStack.size() - 1; i >= 0; i--) {
            StackTraceElement frame = callStack.get(i);

            if (criteria.test(frame))
                return frame;
        }

        return null; // no frames matching the given criteria
    }

    public StackTraceElement leastRecentReflectionCall() {
        return leastRecentCall(ContextResolution::isReflectionFrame);
    }

    public StackTraceElement leastRecentNonReflectionCall() {
        return leastRecentCall(frame -> !ContextResolution.isReflectionFrame(frame));
    }

    public StackTraceElement leastRecentNativeCall() {
        return leastRecentCall(StackTraceElement::isNativeMethod);
    }

    public StackTraceElement leastRecentNonNativeCall() {
        return leastRecentCall(frame -> !frame.isNativeMethod());
    }

    public StackTraceElement leastRecentNonReflectionNonNativeCall() {
        return leastRecentCall(frame -> !ContextResolution.isReflectionFrame(frame) && !frame.isNativeMethod());
    }

    public boolean containsReflectionCalls() {
        return mostRecentReflectionCall() != null;
    }

    public boolean containsNativeCalls() {
        return mostRecentNativeCall() != null;
    }

}
