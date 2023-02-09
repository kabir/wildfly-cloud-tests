/*
 * JBoss, Home of Professional Open Source.
 *  Copyright 2022 Red Hat, Inc., and individual contributors
 *  as indicated by the @author tags.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.wildfly.test.cloud.common;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

class WildFlyIntegrationTestConfigDelegate {

    private static final String NAMESPACE_STRATEGY = "wildfly.test.namespace.strategy";
    private static final String NAMESPACE_STRATEGY_RANDOM = "random";

    private final String namespace;
    private final List<KubernetesResource> kubernetesResources;
    private final Map<String, ConfigPlaceholderReplacer> placeholderReplacements;
    private final ExtraTestSetup extraTestSetup;

    private WildFlyIntegrationTestConfigDelegate(
            String namespace,
            KubernetesResource[] kubernetesResources,
            ConfigPlaceholderReplacement[] placeholderReplacements,
            Class<? extends ExtraTestSetup> additionalTestSetupClass) {
        this.namespace = namespace;
        this.kubernetesResources = new ArrayList<>(Arrays.asList(kubernetesResources));
        Map<String, ConfigPlaceholderReplacer> replacements = new LinkedHashMap<>();

        try {
            extraTestSetup = (additionalTestSetupClass == null) ?
                    null : additionalTestSetupClass.getDeclaredConstructor().newInstance();
            for (ConfigPlaceholderReplacement replacement : placeholderReplacements) {
                replacements.put(replacement.placeholder(), replacement.replacer().getDeclaredConstructor().newInstance());
            }
        } catch (Exception e) {
            throw new RuntimeException();
        }
        this.placeholderReplacements = replacements;
    }

    static WildFlyIntegrationTestConfigDelegate create(WildFlyKubernetesIntegrationTest annotation) {
        String namespace = namespaceFromStrategy(annotation.namespace());
        return new WildFlyIntegrationTestConfigDelegate(
                namespace, annotation.kubernetesResources(), annotation.placeholderReplacements(), annotation.extraTestSetup());
    }

    static WildFlyIntegrationTestConfigDelegate create(WildFlyOpenshiftIntegrationTest annotation) {
        String namespace = namespaceFromStrategy("");
        return new WildFlyIntegrationTestConfigDelegate(
                namespace, annotation.kubernetesResources(), annotation.placeholderReplacements(), annotation.extraTestSetup());
    }

    private static String namespaceFromStrategy(String namespaceFromAnnotation) {
        String namespaceStrategy = System.getProperty(NAMESPACE_STRATEGY);
        if (namespaceStrategy == null) {
            return namespaceFromAnnotation;
        } else if (namespaceStrategy.equals(NAMESPACE_STRATEGY_RANDOM)) {
            System.out.println("Selected namespace strategy:" + NAMESPACE_STRATEGY_RANDOM);
            // TODO - for the strimzi test we will need to change the namespace
            // used by the test. Also, we will need to massage the URL it uses
            // to download the massive YAML

            // TODO - find a better way to do this. GUIDs feel a bit long
            String ns = "wf-test" + System.currentTimeMillis();
            System.out.println("Calculated namespace name: " + ns);
            return ns;
        }
        throw new IllegalStateException("Unknown value for -D" + NAMESPACE_STRATEGY + ":" + NAMESPACE_STRATEGY_RANDOM);
    }

    public String getNamespace() {
        return namespace;
    }

    public List<KubernetesResource> getKubernetesResources() {
        return kubernetesResources;
    }

    public Map<String, ConfigPlaceholderReplacer> getPlaceholderReplacements() {
        return placeholderReplacements;
    }

    public ExtraTestSetup getExtraTestSetup() {
        return extraTestSetup;
    }

    void addAdditionalKubernetesResources(List<KubernetesResource> additionalKubernetesResources) {
        //Since the additional resources are likely needed by the actual deployment,
        //add them first
        List<KubernetesResource> temp = new ArrayList<>(additionalKubernetesResources);
        temp.addAll(kubernetesResources);

        kubernetesResources.clear();
        kubernetesResources.addAll(temp);
    }
}
