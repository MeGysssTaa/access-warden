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

public final class ContextResolution {

    private ContextResolution() {}

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

    public static void ensureCallPermitted(RestrictedCall.Configuration conf) throws UnexpectedSetupException {
        if (conf == null)
            throw new NullPointerException("conf cannot be null");

        FilteredContext ctx = resolve(conf.contextResolutionOptions());
        ensureCallPermitted(ctx, conf);
    }

    public static void ensureCallPermitted(FilteredContext ctx,
                                           RestrictedCall.Configuration conf) throws UnexpectedSetupException {
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

    public static boolean isCtxResFrame(StackTraceElement frame) {
        return frame.getClassName().equals(ContextResolution.class.getName());
    }

    public static boolean isReflectionFrame(StackTraceElement frame) {
        return frame.getClassName().startsWith("java.lang.reflect.")
            || frame.getClassName().startsWith("sun.reflect."      );
    }

    public static final class Options {
        private Options() {}

        public static final int FILTER_CONTEXT_RESOLUTION = 0b1;
        public static final int FILTER_REFLECTION_FRAMES  = 0b10;
        public static final int FILTER_NATIVE_FRAMES      = 0b100;
        public static final int FILTER_RESOLUTION_CALLER  = 0b1000;
    }

}
