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

import java.util.ArrayList;
import java.util.List;

/**
 * Provides low-level access to context resolution and call stack inspection.
 * <p>
 * Normally, you should not use this class yourself.
 * It is intended for internal use by other Access Warden modules.
 */
public final class ContextResolution {

    private ContextResolution() {}

    /**
     * Retrieves the current call stack (stack trace), and filters out particular
     * elements of it, depending on provided resolution options.
     *
     * @param options optional resolution options - call stack filter parameters bitfield.
     *                See {@link Options} for the list of available options. To combine
     *                multiple options, use the bitwise OR operator ("|").
     *
     * @return a {@link FilteredContext} object that represents the current stack, with maybe
     *         particular parts filtered out (removed) as per the specified resolution options bitfield.
     *
     * @throws UnexpectedSetupException if something is wrong with the current call stack. For example,
     *                                  it is completely empty. Or, if it <i>became empty</i> after being
     *                                  filtered as per the specified resolution options bitfield (for
     *                                  instance, an UnexpectedSetupException will be thrown if the
     *                                  call stack entirely consists of reflection calls, and the
     *                                  provided options int specifies to filter all reflection frames).
     *                                  Another possible case is that the current call stack is <i>too big</i>.
     *
     * @see FilteredContext
     */
    public static FilteredContext resolve(int options) throws UnexpectedSetupException {
        StackTraceElement[] allFrames = new Exception().getStackTrace();
        List<StackTraceElement> callStack = new ArrayList<>();

        boolean filterCtxRes     = (options & Options.FILTER_CONTEXT_RESOLUTION) != 0;
        boolean filterReflection = (options & Options.FILTER_REFLECTION_FRAMES ) != 0;
        boolean filterNative     = (options & Options.FILTER_NATIVE_FRAMES     ) != 0;
        boolean filterResCaller  = (options & Options.FILTER_RESOLUTION_CALLER ) != 0;

        int framesPastCtxResFrames = 0;

        for (StackTraceElement frame : allFrames) {
            if (isCtxResFrame(frame)) {
                if (filterCtxRes)
                    continue;
            } else
                framesPastCtxResFrames++;

            if (filterResCaller && framesPastCtxResFrames == 1)
                continue;

            if ((filterReflection && isReflectionFrame(frame))
                    || (filterNative && frame.isNativeMethod()))
                continue;

            callStack.add(frame);
        }

        return new FilteredContext(callStack);
    }

    /**
     * Resolves the current call stack and checks if it should be
     * allowed or not based on the specified configuration. This
     * method throws a {@link java.lang.SecurityException} if the
     * current call stack violates the specified rules/configuration.
     *
     * @param conf the configuration to follow.
     *
     * @throws NullPointerException if {@code conf} is {@code null}.
     *
     * @throws UnexpectedSetupException if something is wrong with the current call stack
     *                                  (see the JavaDoc to {@link #resolve(int) for details}.
     *
     * @see RestrictedCall
     *
     * @see #ensureCallPermitted(FilteredContext, RestrictedCall.Configuration)
     */
    public static void ensureCallPermitted(RestrictedCall.Configuration conf) throws UnexpectedSetupException {
        if (conf == null)
            throw new NullPointerException("conf cannot be null");

        FilteredContext ctx = resolve(conf.contextResolutionOptions());
        ensureCallPermitted(ctx, conf);
    }

    /**
     * Checks if the specified (predefined) call stack should be
     * allowed or not based on the specified configuration. This
     * method throws a {@link java.lang.SecurityException} if the
     * given call stack violates the specified rules/configuration.
     *
     * @param ctx the current call stack (see {@link #resolve(int)}).
     *
     * @param conf the configuration to follow.
     *
     * @throws NullPointerException if {@code ctx} is {@code null}, or if {@code conf} is {@code null}.
     *
     * @throws UnexpectedSetupException if something is wrong with the current call stack
     *                                  (see the JavaDoc to {@link #resolve(int) for details}.
     *
     * @see RestrictedCall
     *
     * @see #ensureCallPermitted(RestrictedCall.Configuration)
     */
    public static void ensureCallPermitted(FilteredContext ctx, RestrictedCall.Configuration conf) {
        if (ctx == null)
            throw new NullPointerException("ctx cannot be null");

        if (conf == null)
            throw new NullPointerException("conf cannot be null");

        if (conf.exactExpectedCallStack().isEmpty())
            generalCallStackCheck(ctx, conf);
        else
            exactCallStackMatchCheck(ctx, conf);
    }

    private static void generalCallStackCheck(FilteredContext ctx, RestrictedCall.Configuration conf) {
        if (conf.prohibitReflectionTraces() && ctx.containsReflectionCalls())
            throw new SecurityException("call not permitted: reflection traces are prohibited");

        if (conf.prohibitNativeTraces() && ctx.containsNativeCalls())
            throw new SecurityException("call not permitted: native traces are prohibited");

        if (conf.prohibitArbitraryInvocation()) {
            StackTraceElement directCaller = ctx.mostRecentNonReflectionNonNativeCall();
            boolean directCallerPermitted = false;

            for (String permittedSrc : conf.permittedSources()) {
                if (Utils.callFrameMatches(directCaller, permittedSrc)) {
                    directCallerPermitted = true;
                    break;
                }
            }

            if (!directCallerPermitted)
                throw new SecurityException("call not permitted: arbitrary invocation is prohibited");
        }

        for (String prohibitedSrc : conf.prohibitedSources()) {
            StackTraceElement prohibitedCaller
                    = ctx.mostRecentCall(frame -> Utils.callFrameMatches(frame, prohibitedSrc));

            if (prohibitedCaller != null)
                throw new SecurityException("call not permitted: invocation source is prohibited");
        }
    }

    private static void exactCallStackMatchCheck(FilteredContext ctx, RestrictedCall.Configuration conf) {
        List<StackTraceElement> callStack = ctx.filteredCallStack();
        List<String> expectedCallStack = conf.exactExpectedCallStack();

        if (callStack.size() != expectedCallStack.size())
            throw new SecurityException("call not permitted: unexpected call stack size");

        for (int i = 0; i < callStack.size(); i++)
            if (!Utils.callFrameMatches(callStack.get(i), expectedCallStack.get(i)))
                throw new SecurityException("call not permitted: unexpected call stack frame");
    }

    static boolean isCtxResFrame(StackTraceElement frame) {
        return frame.getClassName().equals(ContextResolution.class.getName());
    }

    static boolean isReflectionFrame(StackTraceElement frame) {
        return frame.getClassName().startsWith("java.lang.reflect.")
            || frame.getClassName().startsWith("sun.reflect."      );
    }

    /**
     * Defines the set of resolution options available for inclusion in the options bitfield.
     * <p>
     * To combine multiple options, for example, FILTER_CONTEXT_RESOLUTION and FILTER_REFLECTION_FRAMES,
     * use the bitwise OR operator ("|"):
     * <p>
     * {@code int options = Options.FILTER_CONTEXT_RESOLUTION | Options.FILTER_REFLECTION_FRAMES;}
     */
    public static final class Options {
        private Options() {}

        /**
         * Removes all stack trace lines that are related to any of the {@link ContextResolution}
         * class methods, such as {@link #resolve(int)}.
         */
        public static final int FILTER_CONTEXT_RESOLUTION = 0b1;

        /**
         * Removes all stack trace lines that are <i>reflection</i> calls, that is,
         * those lines where the caller class is located in one of the following packages:
         * {@code java.lang.reflect} or {@code sun.reflect}.
         */
        public static final int FILTER_REFLECTION_FRAMES  = 0b10;

        /**
         * Removes all stack trace lines that are <i>native (non-Java, for example, C/C++)</i> calls,
         * that is, those lines where the caller line number is set to {@code -2} as per Java API.
         */
        public static final int FILTER_NATIVE_FRAMES      = 0b100;

        /**
         * Removes exactly one line from the stack trace that indicates the <i>caller method</i>
         * of the {@link #resolve(int)} method (note that this is usually <i>not</i>
         * the very first (the most recent) stack trace frame in the call stack).
         */
        public static final int FILTER_RESOLUTION_CALLER  = 0b1000;
    }

}
