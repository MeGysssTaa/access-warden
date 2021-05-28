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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Collections;
import java.util.List;

/**
 * Indicates that the method this annotation is put on should be protected with Access Warden.
 * This annotation specifies the set of rules that all callers to the method must follow (or
 * all the criteria they must match), and is transformed into checker bytecode by the
 * <a href="https://github.com/MeGysssTaa/access-warden/wiki/The-Core-module">Access Warden Core module</a>.
 * <p>
 * For details on configuring the annotation and examples see
 * <a href="https://github.com/MeGysssTaa/access-warden/wiki/The-API-module">the RestrictedCall page of the wiki</a>.
 */
@Target (ElementType.METHOD)
@Retention (RetentionPolicy.RUNTIME)
public @interface RestrictedCall {

    /**
     * Whether the annotation put on the method should be kept by Access Warden transformer
     * at runtime or not.
     * <p>
     * Most of the times, you'll want to be removing the annotation after transformation, except for cases
     * when you want to explicitly show to the users of your application API that this method has a strictly
     * defined set of rules that must be followed for it to be invoked.
     */
    boolean preserveThisAnnotation() default false;
    String k_preserveThisAnnotation = "preserveThisAnnotation";

    /**
     * The exact way the call stack (list of Strings of format "fully.qualified.ClassName#methodName") must
     * look when invoking this method, in reverse-chronological order (that is, most recent call first). This
     * list must not include the annotated (protected) method itself.
     * <p>
     * If empty, this parameter ("paranoid mode", basically) will not be used. Otherwise, if set,
     * <i>all</i> other annotation parameters will have no effect.
     * <p>
     * Elements of this list support basic glob wildcards ("?" and "*").
     */
    String[] exactExpectedCallStack() default {};
    String k_exactExpectedCallStack = "exactExpectedCallStack";

    /**
     * Whether to forbid <i>any</i> call traces of the Java Reflections API to be present in the call stack.
     */
    boolean prohibitReflectionTraces() default false;
    String k_prohibitReflectionTraces = "prohibitReflectionTraces";

    /**
     * Whether to forbid <i>any</i> call traces of native (C/C++) code to be present in the call stack.
     */
    boolean prohibitNativeTraces() default false;
    String k_prohibitNativeTraces = "prohibitNativeTraces";

    /**
     * Whether the <i>most recent call frame</i> of the call stack must be strictly
     * limited to a predefined set of sources.
     * <p>
     * Only has effect if the list of permittedSources if not empty.
     *
     * @see #permittedSources()
     */
    boolean prohibitArbitraryInvocation() default false;
    String k_prohibitArbitraryInvocation = "prohibitArbitraryInvocation";

    /**
     * Defines the list of <i>only</i> allowed direct callers of this method.
     * <p>
     * Elements of this list support basic glob wildcards ("?" and "*").
     *
     * @see #prohibitArbitraryInvocation()
     */
    String[] permittedSources() default {};
    String k_permittedSources = "permittedSources";

    /**
     * Defines the list of explicitly <i>banned</i> direct callers of this method, that is,
     * the list of sources that are <i>forbidden</i> from being the <i>most recent call frame</i> in the call stack.
     * <p>
     * Elements of this list support basic glob wildcards ("?" and "*").
     */
    String[] prohibitedSources() default {};
    String k_prohibitedSources = "prohibitedSources";

    /**
     * Wraps up the parameters of {@link RestrictedCall} in a convenient
     * method with extra configuration validation.
     * <p>
     * Usually, you will not be creating Configuration objects yourself - the Access Warden Core module
     * will do that for you based on the annotations you provided in your application source code.
     * <p>
     * Methods of the class may throw a {@link UnexpectedSetupException} if the configuration is generally
     * invalid, if some of its particular options are invalid, or if some of the configuration options are
     * <i>contradictory</i> to each other.
     */
    final class Configuration {
        private static final int MAX_LISTS_SIZE = 1000;

        private List<String> exactExpectedCallStack;
        private boolean      prohibitReflectionTraces;
        private boolean      prohibitNativeTraces;
        private boolean      prohibitArbitraryInvocation;
        private List<String> permittedSources;
        private List<String> prohibitedSources;

        private int contextResolutionOptions;

        private Configuration() {}

        public List<String> exactExpectedCallStack() {
            return exactExpectedCallStack;
        }

        public boolean prohibitReflectionTraces() {
            return prohibitReflectionTraces;
        }

        public boolean prohibitNativeTraces() {
            return prohibitNativeTraces;
        }

        public boolean prohibitArbitraryInvocation() {
            return prohibitArbitraryInvocation;
        }

        public int contextResolutionOptions() {
            return contextResolutionOptions;
        }

        public static Builder newBuilder() {
            return new Builder();
        }

        public List<String> permittedSources() {
            return permittedSources;
        }

        public List<String> prohibitedSources() {
            return prohibitedSources;
        }

        public static final class Builder {
            private final Configuration target;

            private Builder() {
                target = new Configuration();
            }

            public Configuration build() throws UnexpectedSetupException {
                validateAndCompleteTarget();
                return target;
            }

            private void validateAndCompleteTarget() throws UnexpectedSetupException {
                completeMissing();
                ensureConfigurationNotContradictory();
                calculateResolutionOptions();
            }

            private void completeMissing() throws UnexpectedSetupException {
                if (target.exactExpectedCallStack == null)
                    target.exactExpectedCallStack = Collections.emptyList();
                else if (target.exactExpectedCallStack.size() > MAX_LISTS_SIZE)
                    throw new UnexpectedSetupException(
                            "the exactExpectedCallStack list is too large ("
                                    + target.exactExpectedCallStack.size() + " > " + MAX_LISTS_SIZE + ")");

                if (target.permittedSources == null)
                    target.permittedSources = Collections.emptyList();
                else if (target.permittedSources.size() > MAX_LISTS_SIZE)
                    throw new UnexpectedSetupException(
                            "the permittedSources list is too large ("
                                    + target.permittedSources.size() + " > " + MAX_LISTS_SIZE + ")");

                if (target.prohibitedSources == null)
                    target.prohibitedSources = Collections.emptyList();
                else if (target.prohibitedSources.size() > MAX_LISTS_SIZE)
                    throw new UnexpectedSetupException(
                            "the prohibitedSources list is too large ("
                                    + target.prohibitedSources.size() + " > " + MAX_LISTS_SIZE + ")");
            }

            private void ensureConfigurationNotContradictory() throws UnexpectedSetupException {
                if (!target.exactExpectedCallStack.isEmpty() &&
                        (target.prohibitReflectionTraces
                                || target.prohibitNativeTraces
                                || target.prohibitArbitraryInvocation
                                || !target.permittedSources.isEmpty()
                                || !target.prohibitedSources.isEmpty()))
                    throw new UnexpectedSetupException(
                            "contradictory configuration: when exactExpectedCallStack is not empty, absolutely all " +
                            "calls, except for those that match the expected call stack exactly, are " +
                            "suspended (TIP: remove exactExpectedCallStack to make the check less strict, " +
                            "or delete all other parameters to make it work in the described 'paranoid' mode)");

                if (target.prohibitArbitraryInvocation && target.permittedSources.isEmpty())
                    throw new UnexpectedSetupException(
                            "contradictory configuration: prohibitArbitraryInvocation " +
                            "is set to true, but the permittedSources list is empty - absolutely all calls " +
                            "are suspended, without a single exception (TIP: populate permittedSources with " +
                            "elements that should be allowed to make this call, or disable " +
                            "prohibitArbitraryInvocation to allow all calls by default)");

                if (!target.prohibitArbitraryInvocation && !target.permittedSources.isEmpty())
                    throw new UnexpectedSetupException(
                            "contradictory configuration: prohibitArbitraryInvocation " +
                            "is set to false, but the permittedSources list is not empty - the specified " +
                            "whitelist will be ignored, and all calls will be allowed by default (TIP: " +
                            "enable prohibitArbitraryInvocation for the whitelist to work, or populate " +
                            "prohibitedSources with these elements to use a blacklist instead)");
            }

            private void calculateResolutionOptions() {
                target.contextResolutionOptions
                        = ContextResolution.Options.FILTER_CONTEXT_RESOLUTION |
                          ContextResolution.Options.FILTER_RESOLUTION_CALLER  ;

                if (target.exactExpectedCallStack.isEmpty() && !target.prohibitReflectionTraces)
                    target.contextResolutionOptions |= ContextResolution.Options.FILTER_REFLECTION_FRAMES;

                if (target.exactExpectedCallStack.isEmpty() && !target.prohibitNativeTraces)
                    target.contextResolutionOptions |= ContextResolution.Options.FILTER_NATIVE_FRAMES;
            }

            public Builder exactExpectedCallStack(List<String> exactExpectedCallStack) {
                target.exactExpectedCallStack = exactExpectedCallStack;
                return this;
            }

            public Builder prohibitReflectionTraces(boolean prohibitReflectionTraces) {
                target.prohibitReflectionTraces = prohibitReflectionTraces;
                return this;
            }

            public Builder prohibitNativeTraces(boolean prohibitNativeTraces) {
                target.prohibitNativeTraces = prohibitNativeTraces;
                return this;
            }

            public Builder prohibitArbitraryInvocation(boolean prohibitArbitraryInvocation) {
                target.prohibitArbitraryInvocation = prohibitArbitraryInvocation;
                return this;
            }

            public Builder permittedSources(List<String> permittedSources) {
                target.permittedSources = permittedSources;
                return this;
            }

            public Builder prohibitedSources(List<String> prohibitedSources) {
                target.prohibitedSources = prohibitedSources;
                return this;
            }
        }
    }

}
