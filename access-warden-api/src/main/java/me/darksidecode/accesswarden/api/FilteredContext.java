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

import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

/**
 * A container object for filtered stack traces (call stacks) with useful methods.
 * Cannot be instantiated manually - use {@link ContextResolution#resolve(int)} instead.
 * <p>
 * Normally, you should not use this class's capabilities yourself,
 * and let Access Warden do all the magic for you internally.
 *
 * @see ContextResolution#resolve(int)
 */
public final class FilteredContext {

    /**
     * The maximum allowed number of call stack traces (stack trace lines)
     * in the backed filtered {@link StackTraceElement} storage.
     */
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

    /**
     * Returns an <i>unmodifiable view</i> of the internal {@code StackTraceElement} storage.
     * <p>
     * Note that separate calls to this method may produce <i>different</i> {@link List} objects,
     * though with the same contents. Using this method instead of other methods provided by
     * {@code FilteredContext} may be an indicator of bad design.
     *
     * @return an <i>unmodifiable view</i> of the internal {@link StackTraceElement} storage.
     */
    public List<StackTraceElement> filteredCallStack() {
        return Collections.unmodifiableList(callStack);
    }

    /**
     * Returns the <i>most</i> recent call of the stack.
     * <p>
     * If the call stack backed by this {@code FilteredContext} consists of exactly one element,
     * then the return value of this method is equal to the return value of
     * {@link #leastRecentCall()}.
     *
     * @return the <i>most</i> recent call of the stack as a {@link StackTraceElement}.
     *         Provided that {@code FilteredContext} internally validates the internal
     *         storage, this method is <i>guaranteed</i> to <i>never</i> return {@code null}.
     */
    public StackTraceElement mostRecentCall() {
        return callStack.get(0);
    }

    /**
     * Returns the <i>most</i> recent call of the stack matching the given criteria.
     * <p>
     * If the call stack backed by this {@code FilteredContext} consists of exactly one element,
     * then the return value of this method is equal to the return value of
     * {@link #leastRecentCall(Predicate)}.
     *
     * @param criteria the criteria the element must match.
     *
     * @return the <i>most</i> recent call of the stack matching the given criteria
     *         as a {@link StackTraceElement} if it exists, or {@code null} if the call
     *         stack represented by this {@code FilteredContext} does not contain <i>any</i>
     *         call frames (stack trace lines) that match the given criteria.
     *
     * @throws NullPointerException if {@code criteria} is {@core null}.
     */
    public StackTraceElement mostRecentCall(Predicate<StackTraceElement> criteria) {
        if (criteria == null)
            throw new NullPointerException("criteria cannot be null");

        for (StackTraceElement frame : callStack)
            if (criteria.test(frame))
                return frame;

        return null; // no frames matching the given criteria
    }

    /**
     * Returns the <i>most</i> recent <i>reflection</i> call of the stack.
     * <p>
     * If the call stack backed by this {@code FilteredContext} consists of exactly one element,
     * then the return value of this method is equal to the return value of
     * {@link #leastRecentReflectionCall()}.
     *
     * @return the <i>most</i> recent <i>reflection</i> call of the stack as a {@link StackTraceElement},
     *         if it exists, or {@code null} if there are no such call frames in the backed storage.
     */
    public StackTraceElement mostRecentReflectionCall() {
        return mostRecentCall(ContextResolution::isReflectionFrame);
    }

    /**
     * Returns the <i>most</i> recent <i>non-reflection</i> call of the stack.
     * <p>
     * If the call stack backed by this {@code FilteredContext} consists of exactly one element,
     * then the return value of this method is equal to the return value of
     * {@link #leastRecentNonReflectionCall()}.
     *
     * @return the <i>most</i> recent <i>non-reflection</i> call of the stack as a {@link StackTraceElement},
     *         if it exists, or {@code null} if there are no such call frames in the backed storage.
     */
    public StackTraceElement mostRecentNonReflectionCall() {
        return mostRecentCall(frame -> !ContextResolution.isReflectionFrame(frame));
    }

    /**
     * Returns the <i>most</i> recent <i>native</i> call of the stack.
     * <p>
     * If the call stack backed by this {@code FilteredContext} consists of exactly one element,
     * then the return value of this method is equal to the return value of
     * {@link #leastRecentNativeCall()}.
     *
     * @return the <i>most</i> recent <i>native</i> call of the stack as a {@link StackTraceElement},
     *         if it exists, or {@code null} if there are no such call frames in the backed storage.
     */
    public StackTraceElement mostRecentNativeCall() {
        return mostRecentCall(StackTraceElement::isNativeMethod);
    }

    /**
     * Returns the <i>most</i> recent <i>non-native</i> call of the stack.
     * <p>
     * If the call stack backed by this {@code FilteredContext} consists of exactly one element,
     * then the return value of this method is equal to the return value of
     * {@link #leastRecentNonNativeCall()}.
     *
     * @return the <i>most</i> recent <i>non-native</i> call of the stack as a {@link StackTraceElement},
     *         if it exists, or {@code null} if there are no such call frames in the backed storage.
     */
    public StackTraceElement mostRecentNonNativeCall() {
        return mostRecentCall(frame -> !frame.isNativeMethod());
    }

    /**
     * Returns the <i>most</i> recent <i>non-reflection, non-native</i> call of the stack.
     * <p>
     * If the call stack backed by this {@code FilteredContext} consists of exactly one element,
     * then the return value of this method is equal to the return value of
     * {@link #leastRecentNonReflectionNonNativeCall()}.
     *
     * @return the <i>most</i> recent <i>non-reflection, non-native</i> call of the stack as a {@link StackTraceElement},
     *         if it exists, or {@code null} if there are no such call frames in the backed storage.
     */
    public StackTraceElement mostRecentNonReflectionNonNativeCall() {
        return mostRecentCall(frame -> !ContextResolution.isReflectionFrame(frame) && !frame.isNativeMethod());
    }

    /**
     * Returns the <i>least</i> recent call of the stack.
     * <p>
     * If the call stack backed by this {@code FilteredContext} consists of exactly one element,
     * then the return value of this method is equal to the return value of
     * {@link #mostRecentCall()}.
     *
     * @return the <i>least</i> recent call of the stack as a {@link StackTraceElement}.
     *         Provided that {@code FilteredContext} internally validates the internal
     *         storage, this method is <i>guaranteed</i> to <i>never</i> return {@code null}.
     */
    public StackTraceElement leastRecentCall() {
        return callStack.get(callStack.size() - 1);
    }

    /**
     * Returns the <i>least</i> recent call of the stack matching the given criteria.
     * <p>
     * If the call stack backed by this {@code FilteredContext} consists of exactly one element,
     * then the return value of this method is equal to the return value of
     * {@link #mostRecentCall()}.
     *
     * @param criteria the criteria the element must match.
     *
     * @return the <i>least</i> recent call of the stack matching the given criteria
     *         as a {@link StackTraceElement} if it exists, or {@code null} if the call
     *         stack represented by this {@code FilteredContext} does not contain <i>any</i>
     *         call frames (stack trace lines) that match the given criteria.
     *
     * @throws NullPointerException if {@code criteria} is {@core null}.
     */
    public StackTraceElement leastRecentCall(Predicate<StackTraceElement> criteria) {
        if (criteria == null)
            throw new NullPointerException("criteria cannot be null");
        
        for (int i = callStack.size() - 1; i >= 0; i--) {
            StackTraceElement frame = callStack.get(i);
            if (criteria.test(frame)) return frame;
        }

        return null; // no frames matching the given criteria
    }

    /**
     * Returns the <i>least</i> recent <i>reflection</i> call of the stack.
     * <p>
     * If the call stack backed by this {@code FilteredContext} consists of exactly one element,
     * then the return value of this method is equal to the return value of
     * {@link #mostRecentReflectionCall()}.
     *
     * @return the <i>least</i> recent <i>reflection</i> call of the stack as a {@link StackTraceElement},
     *         if it exists, or {@code null} if there are no such call frames in the backed storage.
     */
    public StackTraceElement leastRecentReflectionCall() {
        return leastRecentCall(ContextResolution::isReflectionFrame);
    }

    /**
     * Returns the <i>least</i> recent <i>non-reflection</i> call of the stack.
     * <p>
     * If the call stack backed by this {@code FilteredContext} consists of exactly one element,
     * then the return value of this method is equal to the return value of
     * {@link #mostRecentNonReflectionCall()}.
     *
     * @return the <i>least</i> recent <i>non-reflection</i> call of the stack as a {@link StackTraceElement},
     *         if it exists, or {@code null} if there are no such call frames in the backed storage.
     */
    public StackTraceElement leastRecentNonReflectionCall() {
        return leastRecentCall(frame -> !ContextResolution.isReflectionFrame(frame));
    }

    /**
     * Returns the <i>least</i> recent <i>native</i> call of the stack.
     * <p>
     * If the call stack backed by this {@code FilteredContext} consists of exactly one element,
     * then the return value of this method is equal to the return value of
     * {@link #mostRecentNativeCall()}.
     *
     * @return the <i>least</i> recent <i>native</i> call of the stack as a {@link StackTraceElement},
     *         if it exists, or {@code null} if there are no such call frames in the backed storage.
     */
    public StackTraceElement leastRecentNativeCall() {
        return leastRecentCall(StackTraceElement::isNativeMethod);
    }

    /**
     * Returns the <i>least</i> recent <i>non-native</i> call of the stack.
     * <p>
     * If the call stack backed by this {@code FilteredContext} consists of exactly one element,
     * then the return value of this method is equal to the return value of
     * {@link #mostRecentNonNativeCall()}.
     *
     * @return the <i>least</i> recent <i>non-native</i> call of the stack as a {@link StackTraceElement},
     *         if it exists, or {@code null} if there are no such call frames in the backed storage.
     */
    public StackTraceElement leastRecentNonNativeCall() {
        return leastRecentCall(frame -> !frame.isNativeMethod());
    }

    /**
     * Returns the <i>least</i> recent <i>non-reflection, non-native</i> call of the stack.
     * <p>
     * If the call stack backed by this {@code FilteredContext} consists of exactly one element,
     * then the return value of this method is equal to the return value of
     * {@link #mostRecentNonReflectionNonNativeCall()}.
     *
     * @return the <i>least</i> recent <i>non-reflection, non-native</i> call of the stack as a {@link StackTraceElement},
     *         if it exists, or {@code null} if there are no such call frames in the backed storage.
     */
    public StackTraceElement leastRecentNonReflectionNonNativeCall() {
        return leastRecentCall(frame -> !ContextResolution.isReflectionFrame(frame) && !frame.isNativeMethod());
    }

    /**
     * Checks if the call stack contains at least one <i>reflection call</i> trace.
     *
     * @return {@code true} if the call stack contains at least one <i>reflection call</i> trace,
     *         {@code false} otherwise.
     */
    public boolean containsReflectionCalls() {
        return mostRecentReflectionCall() != null;
    }

    /**
     * Checks if the call stack contains at least one <i>native call</i> trace.
     *
     * @return {@code true} if the call stack contains at least one <i>native call</i> trace,
     *         {@code false} otherwise.
     */
    public boolean containsNativeCalls() {
        return mostRecentNativeCall() != null;
    }

}
